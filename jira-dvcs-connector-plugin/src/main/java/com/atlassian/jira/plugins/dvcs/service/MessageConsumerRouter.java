package com.atlassian.jira.plugins.dvcs.service;

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import net.java.ao.DBParam;
import net.java.ao.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageConsumerMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageTagMapping;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessageKey;
import com.atlassian.jira.plugins.dvcs.service.message.MessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.plugin.PluginException;
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
final class MessageConsumerRouter<P> implements Runnable
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
    private final BlockingQueue<Message<P>> messageQueue = new LinkedBlockingQueue<Message<P>>(1);

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
            // FIXME: AO can not be access immediately - it is available lazy
            try
            {
                activeObjects.count(MessageMapping.class);

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

            while (!stop)
            {
                MessageMapping[] founded;
                synchronized (MESSAGE_TRIGGER)
                {
                    Query query = Query.select().distinct().from(MessageMapping.class).alias(MessageMapping.class, "message") //
                            .join(MessageConsumerMapping.class, "message.ID = consumer." + MessageConsumerMapping.MESSAGE) //
                            .alias(MessageConsumerMapping.class, "consumer") //
                            .where( //
                            "message." + MessageMapping.KEY + " = ? AND consumer." + MessageConsumerMapping.CONSUMER + " = ? " //
                                    + " AND consumer." + MessageConsumerMapping.QUEUED + " = ?  " //
                                    + " AND consumer." + MessageConsumerMapping.WAIT_FOR_RETRY + " = ? ", //
                            key.getId(), delegate.getId(), false, false //
                            );
                    founded = activeObjects.find(MessageMapping.class, query);

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

                    try
                    {
                        P payload = payloadSerializer.deserialize(foundedItem.getPayload());
                        String tags[] = new String[foundedItem.getTags().length];
                        for (int i = 0; i < tags.length; i++)
                        {
                            tags[i] = foundedItem.getTags()[i].getTag();
                        }

                        markQueued(foundedItem);
                        messageQueue.put(new Message<P>(foundedItem.getID(), payload, tags));

                    } catch (InterruptedException e)
                    {
                        if (!stop)
                        {
                            throw new RuntimeException(e);

                        }

                    } catch (Exception e)
                    {
                        LOGGER.error(e.getMessage(), e);
                        fail(foundedItem.getID());

                    }
                }
            }
        }

        /**
         * Marks messages as queued for processing.
         * 
         * @param message
         *            for marking
         */
        private void markQueued(MessageMapping message)
        {
            for (MessageConsumerMapping consumer : message.getConsumers())
            {
                if (consumer.getConsumer().equals(delegate.getId()))
                {
                    consumer.setQueued(true);
                    consumer.save();
                    break;
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
     *            #getDe
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
     * @return delegate for messages processing
     */
    public MessageConsumer<P> getDelegate()
    {
        return delegate;
    }

    /**
     * Routes provided message information.
     * 
     * @param messageId
     *            identity of message
     * @param payload
     *            of message
     * @param tags
     *            of message
     */
    public void route(final int messageId, final P payload, final String... tags)
    {

        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                activeObjects.create(MessageConsumerMapping.class, //
                        new DBParam(MessageConsumerMapping.MESSAGE, messageId), //
                        new DBParam(MessageConsumerMapping.CONSUMER, delegate.getId()), //
                        new DBParam(MessageConsumerMapping.QUEUED, Boolean.FALSE), //
                        new DBParam(MessageConsumerMapping.WAIT_FOR_RETRY, Boolean.FALSE), //
                        new DBParam(MessageConsumerMapping.RETRIES_COUNT, 0) //
                        );

                for (String tag : tags)
                {
                    activeObjects.create(MessageTagMapping.class, //
                            new DBParam(MessageTagMapping.MESSAGE, messageId), //
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
     * @see MessagingService#ok(MessageConsumer, int)
     * @param messageId
     */
    public void ok(final int messageId)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                MessageMapping message = activeObjects.get(MessageMapping.class, messageId);
                List<MessageConsumerMapping> consumers = new LinkedList<MessageConsumerMapping>(Arrays.asList(message.getConsumers()));
                Iterator<MessageConsumerMapping> consumersIterator = consumers.iterator();

                while (consumersIterator.hasNext())
                {
                    MessageConsumerMapping messageConsumer = consumersIterator.next();
                    if (messageConsumer.getConsumer().equals(delegate.getId()))
                    {
                        activeObjects.delete(messageConsumer);
                        consumersIterator.remove();
                        break;
                    }
                }

                if (consumers.isEmpty())
                {
                    for (MessageTagMapping messageTag : message.getTags())
                    {
                        activeObjects.delete(messageTag);
                    }
                    activeObjects.delete(message);
                }

                return null;
            }

        });
    }

    /**
     * @see MessagingService#fail(MessageConsumer, int)
     * @param messageId
     */
    public void fail(final int messageId)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                MessageMapping message = activeObjects.get(MessageMapping.class, messageId);
                for (MessageConsumerMapping messageConsumer : message.getConsumers())
                {
                    if (messageConsumer.getConsumer().equals(delegate.getId()))
                    {
                        messageConsumer.setLastFailed(new Date());
                        messageConsumer.setWaitForRetry(true);
                        messageConsumer.setRetriesCount(messageConsumer.getRetriesCount() + 1);
                        messageConsumer.save();
                        break;
                    }
                }

                return null;
            }

        });
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
        Query query = Query
                .select()
                .from(MessageMapping.class)
                .alias(MessageMapping.class, "message")
                .join(MessageConsumerMapping.class, "message.ID = consumer." + MessageConsumerMapping.MESSAGE)
                .join(MessageTagMapping.class, "message.ID = tag." + MessageTagMapping.MESSAGE)
                .alias(MessageConsumerMapping.class, "consumer")
                .alias(MessageTagMapping.class, "tag")
                .where("message." + MessageMapping.KEY + " = ? AND consumer." + MessageConsumerMapping.CONSUMER
                        + " = ? AND tag.tag = ? AND consumer." + MessageConsumerMapping.QUEUED + " = ? AND consumer."
                        + MessageConsumerMapping.WAIT_FOR_RETRY + "  = ?", key.getId(), delegate.getId(), tag, false, false);
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
            Message<P> message = null;
            try
            {
                message = messageQueue.take();
                delegate.onReceive(message.getId(), message.getPayload(), message.getTags());

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
