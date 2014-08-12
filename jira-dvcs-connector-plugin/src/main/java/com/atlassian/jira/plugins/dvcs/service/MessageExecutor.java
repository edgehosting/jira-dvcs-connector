package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.beehive.ClusterLockService;
import com.atlassian.beehive.compat.ClusterLockServiceFactory;
import com.atlassian.jira.plugins.dvcs.event.RepositorySync;
import com.atlassian.jira.plugins.dvcs.event.RepositorySyncHelper;
import com.atlassian.jira.plugins.dvcs.model.DiscardReason;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.AbstractMessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.service.message.HasProgress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;
import com.atlassian.util.concurrent.ThreadFactories;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import static com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag.SOFT_SYNC;
import static com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag.WEBHOOK_SYNC;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Is responsible for message execution.
 *
 * @author Stanislav Dvorscak
 */
public class MessageExecutor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumer.class);

    private static final String PROCESS_MESSAGE_LOCK = MessageExecutor.class.getName() + ".processMessage";

    /**
     * Executor that is used for consumer execution.
     */
    private final ExecutorService executor;

    private ClusterLockService clusterLockService;

    @Resource
    private ClusterLockServiceFactory clusterLockServiceFactory;

    @Resource
    private MessagingService messagingService;

    @Resource
    @SuppressWarnings ("MismatchedReadAndWriteOfArray")
    private MessageConsumer<?>[] consumers;

    /**
     * Used to capture & store events raised during synchronisation.
     */
    @Resource
    private RepositorySyncHelper repoSyncHelper;

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
    private volatile boolean stop;

    /**
     * Creates a new MessageExecutor backed by a thread pool.
     */
    public MessageExecutor()
    {
        this(createThreadPoolExecutor());
    }

    /**
     * Creates a new MessageExecutor backed by the given ExecutorService.
     *
     * @param executor    an ExecutorService
     */
    @VisibleForTesting
    public MessageExecutor(@Nonnull ExecutorService executor)
    {
        this.executor = checkNotNull(executor, "executor");
    }

    /**
     * Initializes this bean.
     */
    @PostConstruct
    public void init()
    {
        clusterLockService = clusterLockServiceFactory.getClusterLockService();
        for (final MessageConsumer<?> consumer : consumers)
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
        // call shutdownNow to interrupt current msg and also ignore the other messages in the queue
        executor.shutdownNow();
        if (!executor.awaitTermination(1, TimeUnit.MINUTES))
        {
            LOGGER.error("Unable properly shutdown message queue.");
        }
    }

    /**
     * Notifies that a message with the given address was added to the queues.
     * It is necessary because of consumers' weak-up, which can be slept
     * because of empty queues.
     *
     * @param address destination address of new message
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
    private <P extends HasProgress> void tryToProcessNextMessage(final MessageConsumer<P> consumer)
    {
        if (stop)
        {
            return;
        }

        Message<P> message;
        final Lock lock = clusterLockService.getLockForName(PROCESS_MESSAGE_LOCK);
        lock.lock();
        try
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
        finally
        {
            lock.unlock();
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
     * Returns a new ThreadPoolExecutor. Due to the way {@link java.util.concurrent.ThreadPoolExecutor} is designed
     * (see the QUEUING section of its javadoc), when using {@link java.util.concurrent.LinkedBlockingQueue}, the 
     * max value has no impact. We would fix it properly in https://jdog.jira-dev.com/browse/BBC-815.
     * <p/>
     * For now, just set it to non-Integer.MAX_VALUE value.
     *
     * @return a new ThreadPoolExecutor
     */
    private static ThreadPoolExecutor createThreadPoolExecutor()
    {
        return new ThreadPoolExecutor(1, Integer.MAX_VALUE, 5, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>(),
                ThreadFactories.namedThreadFactory("DVCSConnector.MessageExecutor"));
    }

    /**
     * Template class for {@code Runnable}s that should release the token after they run and enqueue the next message.
     */
    private abstract class ReleaseTokenAndEnqueueNextMessage implements Runnable
    {
        /**
         * Delegate to subclass then release token.
         */
        @Override
        public final void run()
        {
            try
            {
                doRun();
            }
            finally
            {
                final MessageConsumer<?> consumer = getConsumer();

                // release the token, then enqueue the next message
                releaseToken(consumer);
                tryToProcessNextMessage(consumer);
            }
        }

        /**
         * Run whatever.
         */
        protected abstract void doRun();

        /**
         * @return the MessageConsumer.
         */
        protected abstract MessageConsumer<?> getConsumer();
    }

    /**
     * Runnable for single message processing.
     *
     * @author Stanislav Dvorscak
     *
     * @param <P>
     *            type of message payload
     */
    private final class MessageRunnable<P extends HasProgress> extends ReleaseTokenAndEnqueueNextMessage
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
         * @param message the message
         * @param consumer the consumer of the message
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

        @Override
        protected void doRun()
        {
            final Progress progress;
            final P payload;
            try
            {
                payload = messagingService.deserializePayload(message);
                progress = payload.getProgress();
            }
            catch (AbstractMessagePayloadSerializer.MessageDeserializationException e)
            {
                messagingService.discard(consumer, message, DiscardReason.FAILED_DESERIALIZATION);
                throw e;
            }

            // listen for sync events during soft sync only to avoid replaying events when accounts are removed and
            // subsequently re-added
            final boolean softSync = progress.isSoftsync() && payload.isSoftSync();
            final boolean webHookSync = progress.isWebHookSync() && payload.isWebHookSync();
            final Repository repository = messagingService.getRepositoryFromMessage(message);

            // reconstruct the sync flags from softSync / webHookSync
            final EnumSet<SynchronizationFlag> syncFlags = EnumSet.noneOf(SynchronizationFlag.class);
            if (softSync) { syncFlags.add(SOFT_SYNC); }
            if (webHookSync) { syncFlags.add(WEBHOOK_SYNC); }

            final RepositorySync repoSync = repoSyncHelper.startSync(repository, syncFlags);
            try
            {
                consumer.onReceive(message, payload);
                messagingService.ok(consumer, message);
            }
            catch (Throwable t)
            {
                LOGGER.error("Synchronization failed: " + t.getMessage(), t);
                messagingService.fail(consumer, message, t);

                if (message.getRetriesCount() >= 3)
                {
                    messagingService.discard(consumer, message, DiscardReason.RETRY_COUNT_EXCEEDED);
                }

                progress.setError("Error during sync. See server logs.");
                Throwables.propagateIfInstanceOf(t, Error.class);
            }
            finally
            {
                repoSync.finish();
                tryEndProgress(repository, message, consumer, progress);
            }
        }

        protected void tryEndProgress(Repository repository, Message<P> message, MessageConsumer<P> consumer, Progress progress)
        {
            try
            {
                if (repository != null)
                {
                    messagingService.tryEndProgress(repository, progress, consumer, messagingService.getSynchronizationAuditIdFromTags(message.getTags()));
                }
            }
            catch (RuntimeException e)
            {
                LOGGER.error(e.getMessage(), e);
                // Any RuntimeException will be ignored in this step
            }
        }
    }

}
