package com.atlassian.jira.plugins.dvcs.service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import net.java.ao.DBParam;
import net.java.ao.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageTagMapping;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessageKey;
import com.atlassian.jira.plugins.dvcs.service.message.MessagePayloadSerializer;
import com.atlassian.sal.api.transaction.TransactionCallback;

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
     * @see #setActiveObjects(ActiveObjects)
     */
    private final ActiveObjects activeObjects;

    /**
     * Each consumer has own thread.
     */
    private final Thread workerThread;

    /**
     * Thread which listens for incoming messages.
     */
    private Thread listenThread;

    /**
     * @see #MessageConsumerRouter(MessageKey, MessageConsumer, MessagePayloadSerializer)
     */
    private final MessageKey<P> key;

    /**
     * @see #MessageConsumerRouter(MessageKey, MessageConsumer, MessagePayloadSerializer)
     */
    private final MessageConsumer<P> delegate;

    /**
     * @see #MessageConsumerRouter(MessageKey, MessageConsumer, MessagePayloadSerializer)
     */
    private final MessagePayloadSerializer<P> payloadSerializer;

    /**
     * @see #stop()
     */
    private boolean stop;

    /**
     * Holds messages determined for this consumer.
     */
    private final BlockingQueue<Message<K, P>> messageQueue = new LinkedBlockingQueue<Message<K, P>>();

    /**
     * News message available.
     */
    public final Object MESSAGE_TRIGGER = new Object();

    /**
     * Responses for message retrieving from database.
     * 
     * @author Stanislav Dvorscak
     * 
     */
    private final class DBQueueFiller implements Runnable
    {

        /**
         * {@inheritDoc}
         */
        @Override
        public void run()
        {
            while (!stop)
            {
                MessageMapping[] founded;
                synchronized (MESSAGE_TRIGGER)
                {
                    founded = activeObjects.find(MessageMapping.class);

                    if (founded.length == 0)
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

                for (MessageMapping foundedItem : founded)
                {

                    P payload = payloadSerializer.deserialize(foundedItem.getPayload());
                    try
                    {
                        messageQueue.put(new Message<K, P>(payload, foundedItem.getTags()));
                    } catch (InterruptedException e)
                    {
                        if (!stop)
                        {
                            throw new RuntimeException(e);

                        }
                    }
                }
            }
        }
    }

    /**
     * Constructor.
     * 
     * @param activeObjects
     * @param key
     *            key Key of messages for which this consumer listens.
     * @param delegate
     *            for messages processing
     * @param payloadSerializer
     *            serializer of {@link Message#getPayload()}
     */
    public MessageConsumerRouter(ActiveObjects activeObjects, MessageKey<P> key, MessageConsumer<P> delegate,
            MessagePayloadSerializer<P> payloadSerializer)
    {
        this.activeObjects = activeObjects;

        this.key = key;
        this.delegate = delegate;
        this.payloadSerializer = payloadSerializer;

        listenThread = new Thread(new DBQueueFiller(), DBQueueFiller.class.getCanonicalName() + "@" + delegate.toString());
        listenThread.start();

        workerThread = new Thread(this, delegate.getKey().getClass().getCanonicalName() + "@" + delegate.toString());
        workerThread.start();
    }

    /**
     * @param message
     *            adds message which is determined for this queue
     */
    public void route(final Message<K, P> message)
    {

        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                MessageMapping messageMapping = activeObjects.create(MessageMapping.class, //
                        new DBParam(MessageMapping.KEY, key.getId()), //
                        new DBParam(MessageMapping.PAYLOAD, payloadSerializer.serialize(message.getPayload())), //
                        new DBParam(MessageMapping.CONSUMER, delegate.getId()) //
                        );

                for (String tag : message.getTags())
                {
                    activeObjects.create(MessageTagMapping.class, //
                            new DBParam(MessageTagMapping.MESSAGE, messageMapping.getID()), //
                            new DBParam(MessageTagMapping.TAG, tag));
                }

                return null;
            }

        });

        // notify that new message was received
        synchronized (MESSAGE_TRIGGER)
        {
            MESSAGE_TRIGGER.notify();
        }
    }

    /**
     * Stops message consumers - e.g.: when shutdown process happened.
     */
    public void stop()
    {
        stop = true;

        listenThread.interrupt();
        try
        {
            listenThread.join();
        } catch (InterruptedException e)
        {
            throw new RuntimeException(e);

        }

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
     * @param tag
     *            message discriminator
     * @return Count of queued messages for provided message tag.
     */
    public int getQueuedCount(String tag)
    {
        Query query = Query.select().from(MessageMapping.class).where( //
                MessageMapping.KEY + " = ? AND " + MessageMapping.CONSUMER + " = ? ", //
                key.getId(), //
                delegate.getId() //
                );
        return activeObjects.count(MessageMapping.class, query);
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
