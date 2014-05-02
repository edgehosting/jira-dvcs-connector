package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.model.DiscardReason;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.AbstractMessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.service.message.HasProgress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * Is responsible for message execution.
 *
 * @author Stanislav Dvorscak
 *
 */
public class MessageExecutor
{

    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumer.class);

    /**
     * Executor, which is used for consumer-s execution.
     */
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 5, TimeUnit.MINUTES,
            new LinkedBlockingQueue<Runnable>())
    {

        protected void afterExecute(Runnable r, Throwable t)
        {
            MessageRunnable<?> messageRunnable = (MessageRunnable<?>) r;
            releaseToken(messageRunnable.getConsumer());
            tryToProcessNextMessage(messageRunnable.getConsumer());
        }

    };

    /**
     * Injected {@link MessagingService} dependency.
     */
    @Resource
    private MessagingService messagingService;

    /**
     * Injected {@link MessageConsumer} dependencies.
     */
    @Resource
    private MessageConsumer<?>[] consumers;

    /**
     * {@link MessageAddress} to appropriate consumers listeners.
     */
    private final ConcurrentMap<String, List<MessageConsumer<?>>> messageAddressToConsumers = new ConcurrentHashMap<String, List<MessageConsumer<?>>>();

    /**
     * {@link MessageConsumer} to free tokens.
     */
    private final ConcurrentMap<MessageConsumer<?>, AtomicInteger> consumerToRemainingTokens = new ConcurrentHashMap<MessageConsumer<?>, AtomicInteger>();

    /**
     * Is messaging stopped?
     */
    private boolean stop;

    /**
     * Initializes this bean.
     */
    @PostConstruct
    public void init()
    {
        for (MessageConsumer<?> consumer : consumers)
        {
            List<MessageConsumer<?>> byAddress = messageAddressToConsumers.get(consumer.getAddress().getId());
            if (byAddress == null)
            {
                messageAddressToConsumers.putIfAbsent(consumer.getAddress().getId(), byAddress = new CopyOnWriteArrayList<MessageConsumer<?>>());
            }
            byAddress.add(consumer);
            consumerToRemainingTokens.put(consumer, new AtomicInteger(consumer.getParallelThreads()));

        }
    }

    /**
     * Stops messaging executor.
     */
    @PreDestroy
    public void destroy() throws Exception
    {
        stop = true;
        executor.shutdown();
        if (!executor.awaitTermination(1, TimeUnit.MINUTES))
        {
            LOGGER.error("Unable properly shutdown message queue.");
        }
    }

    /**
     * Notifies that new message with provided address was added into the queues. It is necessary because of consumers' weak-up, which can
     * be slept because of empty queues.
     *
     * @param address
     *            destination address of new message
     */
    public void notify(String address)
    {
        for (MessageConsumer<?> byMessageAddress : messageAddressToConsumers.get(address))
        {
            tryToProcessNextMessage(byMessageAddress);
        }
    }

    /**
     * Tries to process next message of queue, if there is any message and any available token ({@link MessageConsumer#getParallelThreads()
     * thread}).
     *
     * @param consumer
     *            for processing
     */
    private <P extends HasProgress> void tryToProcessNextMessage(MessageConsumer<P> consumer)
    {
        if (stop)
        {
            return;
        }

        Message<P> message;
        synchronized (this)
        {
            message = messagingService.getNextMessageForConsuming(consumer, consumer.getAddress().getId());

            if (message == null)
            {
                // no other message for processing
                return;
            }

            if (!acquireToken(consumer))
            {
                // no free token/thread for execution
                return;
            }

            // we have token and message - message is going to be marked that is queued / busy - and can be proceed
            messagingService.running(consumer, message);
        }

        // process message itself
        executor.execute(new MessageRunnable<P>(message, consumer));
    }

    /**
     * Gets single available token, if any.
     *
     * @param consumer
     *            which need token
     * @return true if free token was acquired - otherwise false is returned
     */
    private <P extends HasProgress> boolean acquireToken(MessageConsumer<P> consumer)
    {
        AtomicInteger remainingTokens = consumerToRemainingTokens.get(consumer);
        int remainingTokensValue;
        do
        {
            remainingTokensValue = remainingTokens.get();
            if (remainingTokensValue <= 0)
            {
                return false;
            }
        } while (!remainingTokens.compareAndSet(remainingTokensValue, remainingTokensValue - 1));

        return true;
    }

    /**
     * Releases single token - counter part of {@link #acquireToken(MessageConsumer)}.
     *
     * @param consumer
     *            for which consumer
     */
    private <P extends HasProgress> void releaseToken(MessageConsumer<P> consumer)
    {
        consumerToRemainingTokens.get(consumer).incrementAndGet();
    }

    /**
     * Runnable for single message processing.
     *
     * @author Stanislav Dvorscak
     *
     * @param <P>
     *            type of message payload
     */
    private final class MessageRunnable<P extends HasProgress> implements Runnable
    {

        /**
         * Message for processing/
         */
        private final Message<P> message;

        /**
         * @see #getConsumer()
         */
        private final MessageConsumer<P> consumer;

        /**
         * Constructor.
         *
         * @param message
         * @param consumer
         */
        public MessageRunnable(Message<P> message, MessageConsumer<P> consumer)
        {
            this.message = message;
            this.consumer = consumer;
        }

        /**
         * @return Consumer for message processing.
         */
        public MessageConsumer<P> getConsumer()
        {
            return consumer;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run()
        {
            Progress progress = null;
            P payload = null;
            try
            {
                try
                {
                    payload = messagingService.deserializePayload(message);
                    progress = payload.getProgress();
                } catch (AbstractMessagePayloadSerializer.MessageDeserializationException e)
                {
                    progress = e.getProgressOrNull();
                    messagingService.discard(consumer, message, DiscardReason.FAILED_DESERIALIZATION);
                    throw e;
                }

                consumer.onReceive(message, payload);
                messagingService.ok(consumer, message);

            } catch (Throwable t)
            {
                LOGGER.error("Synchronization failed: " + t.getMessage(), t);
                messagingService.fail(consumer, message, t);

                if (message.getRetriesCount() >= 3)
                {
                    messagingService.discard(consumer, message, DiscardReason.RETRY_COUNT_EXCEEDED);
                }

                if (progress != null)
                {
                    progress.setError("Error during sync. See server logs.");
                }
                Throwables.propagateIfInstanceOf(t, Error.class);
            } finally
            {
                tryEndProgress(message, consumer, progress);
            }
        }

        protected void tryEndProgress(Message<P> message, MessageConsumer<P> consumer, Progress progress)
        {
            try
            {
                Repository repository = messagingService.getRepositoryFromMessage(message);
                if (repository != null)
                {
                    messagingService.tryEndProgress(repository, progress, consumer, messagingService.getSynchronizationAuditIdFromTags(message.getTags()));
                }
            } catch (RuntimeException e)
            {
                LOGGER.error(e.getMessage(), e);
                // Any RuntimeException will be ignored in this step
            }
        }
    }

}
