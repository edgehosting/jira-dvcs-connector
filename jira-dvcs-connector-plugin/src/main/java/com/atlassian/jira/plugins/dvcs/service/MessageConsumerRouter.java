package com.atlassian.jira.plugins.dvcs.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageMapping;
import com.atlassian.jira.plugins.dvcs.dao.MessageDao;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.service.message.HasProgress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessageKey;
import com.atlassian.jira.plugins.dvcs.service.message.MessagePayloadSerializer;
import com.atlassian.plugin.PluginException;

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
final class MessageConsumerRouter<P extends HasProgress> implements Runnable
{

    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumer.class);

    /**
     * Injected {@link ActiveObjects} dependency.
     */
    private final ActiveObjects activeObjects;

    /**
     * Injected {@link MessageDao} dependency.
     */
    private final MessageDao messageDao;

    /**
     * Each consumer has own thread.
     */
    private final Thread workerThread;

    /**
     * @see #MessageConsumerRouter(MessageKey, MessageConsumer, MessagePayloadSerializer)
     */
    private final MessageConsumer<P> delegate;

    /**
     * @see #stop()
     */
    private boolean stop;

    /**
     * News message available.
     */
    public final Object MESSAGE_TRIGGER = new Object();

    /**
     * Constructor.
     *
     * @param activeObjects
     * @param messageDao
     * @param key
     *            key Key of messages for which this consumer listens.
     * @param delegate
     */
    public MessageConsumerRouter(ActiveObjects activeObjects, MessageDao messageDao, MessageKey<P> key, MessageConsumer<P> delegate)
    {
        this.activeObjects = activeObjects;
        this.messageDao = messageDao;
        this.delegate = delegate;

        workerThread = new Thread(this, delegate.getKey().getClass().getCanonicalName() + "@" + delegate.toString());
        workerThread.start();
    }

    /**
     * @return delegate for messages processing
     */
    public MessageConsumer<P> getDelegate()
    {
        return delegate;
    }

    /**
     * Routes provided message information.
     *
     * @param message
     *            for processing
     */
    public void route(Message<P> message)
    {
        // notify that new message was received
        synchronized (MESSAGE_TRIGGER)
        {
            MESSAGE_TRIGGER.notify();
        }
    }

    void discard(Message<P> message, P payload)
    {
        messageDao.delete(message);
        payload.getProgress().setFinished(true);
    }

    /**
     * Stops message consumers - e.g.: when shutdown process happened.
     */
    public void stop()
    {
        stop = true;

        workerThread.interrupt();
        try
        {
            workerThread.join();

        } catch (InterruptedException e)
        {
            throw new RuntimeException(e);

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run()
    {
        waitForAo();

        while (!stop)
        {
            Message<P> message;
            synchronized (MESSAGE_TRIGGER)
            {
                message = messageDao.getNextMessageForConsuming(delegate.getKey().getId(), delegate.getId());

                if (message == null)
                {
                    try
                    {
                        MESSAGE_TRIGGER.wait();
                    } catch (InterruptedException e)
                    {
                        if (!stop)
                        {
                            throw new RuntimeException(e);

                        }

                    }

                    continue;
                }
            }

            try
            {

                messageDao.markQueued(message);
                if (message.getPayload().getProgress().isShouldStop())
                {
                    messageDao.delete(message);
                    continue;
                }
                if (!delegate.shouldDiscard(message.getId(), message.getRetriesCount(), message.getPayload(), message.getTags()))
                {
                    delegate.onReceive(message);
                } else
                {
                    discard(message, message.getPayload());
                    delegate.afterDiscard(message.getId(), message.getRetriesCount(), message.getPayload(), message.getTags());
                }

            } catch (Exception e)
            {
                LOGGER.error(e.getMessage(), e);
                messageDao.markFail(message, delegate);
                // TODO what now with progress ?

            }
        }
    }

    private void waitForAo()
    {
        boolean aoInitialized = false;
        do
        {
            try
            {
                LOGGER.debug("Attempting to wait for AO.");
                activeObjects.count(MessageMapping.class);
                aoInitialized = true;
            } catch (PluginException e)
            {
                try
                {
                    Thread.sleep(5000);
                } catch (InterruptedException ie)
                {
                    // nothing to do
                }

            }
        } while (!aoInitialized);
        LOGGER.debug("Attempting to wait for AO - DONE.");
    }

}
