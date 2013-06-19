package com.atlassian.jira.plugins.dvcs.service;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessageKey;
import com.atlassian.jira.plugins.dvcs.service.message.MessageTag;

/**
 * Routes messages to consumer.
 * 
 * @author Stanislav Dvorscak
 * 
 * @param <K>
 *            key of message
 * @param <P>
 *            type of message
 */
final class MessageConsumerRouter<K extends MessageKey<P>, P> implements Runnable
{

    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumer.class);

    /**
     * Message tag to count of remaining/queued.
     */
    private final Map<MessageTag, Integer> tagToQueuedCount = new ConcurrentHashMap<MessageTag, Integer>();

    /**
     * Each consumer has own thread.
     */
    private final Thread thread;

    /**
     * @see #getDelegate()
     */
    private final MessageConsumer<P> delegate;

    /**
     * @see #stop()
     */
    private boolean stop;

    /**
     * Holds messages determined for this consumer.
     */
    private final BlockingQueue<Message<K, P>> messageQueue = new LinkedBlockingQueue<Message<K, P>>();

    /**
     * Constructor.
     * 
     * @param delegate
     *            {@link #getDelegate()}
     */
    public MessageConsumerRouter(MessageConsumer<P> delegate)
    {
        this.delegate = delegate;

        thread = new Thread(this, delegate.getKey().getClass().getCanonicalName() + "@" + delegate.toString());
        thread.start();
    }

    /**
     * @return Original consumer of messages.
     */
    public MessageConsumer<P> getDelegate()
    {
        return delegate;
    }

    /**
     * @param message
     *            adds message which is determined for this queue
     */
    public void route(Message<K, P> message)
    {
        synchronized (tagToQueuedCount)
        {
            for (MessageTag tag : message.getTags())
            {
                Integer count = tagToQueuedCount.get(tag);
                if (count == null)
                {
                    tagToQueuedCount.put(tag, 1);

                } else
                {
                    tagToQueuedCount.put(tag, count + 1);
                }
            }
        }

        try
        {
            messageQueue.put(message);

        } catch (InterruptedException e)
        {
            throw new RuntimeException(e);

        }
    }

    /**
     * Stops message consumers - e.g.: when shutdown process happened.
     */
    public void stop()
    {
        stop = true;
        thread.interrupt();
        try
        {
            thread.join();

        } catch (InterruptedException e)
        {
            throw new RuntimeException(e);

        }
    }

    /**
     * @param tag
     *            message discriminator
     * @return Count of queued messages for provided message tag.
     */
    public int getQueuedCount(MessageTag tag)
    {
        Integer result = tagToQueuedCount.get(tag);
        return result != null ? result : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run()
    {
        while (!stop)
        {
            Message<K, P> message = null;
            try
            {
                message = messageQueue.take();

                synchronized (tagToQueuedCount)
                {
                    if (message != null)
                    {
                        for (MessageTag tag : message.getTags())
                        {
                            Integer count = tagToQueuedCount.get(tag);
                            count--;
                            if (count == 0)
                            {
                                tagToQueuedCount.remove(tag);

                            } else
                            {
                                tagToQueuedCount.put(tag, count);

                            }
                        }
                    }
                }

                delegate.onReceive(message.getPayload());

            } catch (InterruptedException e)
            {
                if (!stop)
                {
                    throw new RuntimeException(e);

                }

            } catch (Exception e)
            {
                LOGGER.error(e.getMessage(), e);

            }
        }
    }

}
