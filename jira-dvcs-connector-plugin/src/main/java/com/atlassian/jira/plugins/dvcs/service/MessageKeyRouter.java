package com.atlassian.jira.plugins.dvcs.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.java.ao.DBParam;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageMapping;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessageKey;
import com.atlassian.jira.plugins.dvcs.service.message.MessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.sal.api.transaction.TransactionCallback;

/**
 * Routes messages to consumers listening on a {@link #getKey()}.
 * 
 * @author Stanislav Dvorscak
 * 
 * @param <K>
 *            type of message key
 * @param <P>
 *            type of message payload
 */
final class MessageKeyRouter<P>
{

    private final ActiveObjects activeObjects;

    /**
     * Consumers listening over a {@link #getKey()}.
     */
    private final Map<String, MessageConsumerRouter<P>> consumers = new ConcurrentHashMap<String, MessageConsumerRouter<P>>();

    /**
     * @see #getKey()
     */
    private final MessageKey<?> key;

    /**
     * @see #MessageKeyRouter(ActiveObjects, MessageKey, MessagePayloadSerializer)
     */
    private MessagePayloadSerializer<P> payloadSerializer;

    /**
     * Constructor.
     * 
     * @param activeObjects
     *            injected {@link ActiveObjects} dependency
     * @param key
     *            messages routed for this key
     * @param payloadSerializer
     *            serializer of message paylaod
     */
    public MessageKeyRouter(ActiveObjects activeObjects, MessageKey<P> key, MessagePayloadSerializer<P> payloadSerializer)
    {
        this.activeObjects = activeObjects;
        this.key = key;
        this.payloadSerializer = payloadSerializer;
    }

    /**
     * Stops messages routing.
     */
    public void stop()
    {
        for (MessageConsumerRouter<?> consumer : consumers.values())
        {
            consumer.stop();
        }
    }

    /**
     * @return over which key is realized routing.
     */
    public MessageKey<?> getKey()
    {
        return key;
    }

    /**
     * Adds routing for {@link #getKey()} and provided message consumer.
     * 
     * @param consumerMessagesRouter
     */
    public void addConsumer(MessageConsumerRouter<P> consumerMessagesRouter)
    {
        consumers.put(consumerMessagesRouter.getDelegate().getId(), consumerMessagesRouter);
    }

    /**
     * Routes provided message to consumers.
     * 
     * @param payload
     *            of message
     * @param tags
     *            of message
     */
    public void route(final P payload, final String... tags)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                MessageMapping messageMapping = activeObjects.create(MessageMapping.class, //
                        new DBParam(MessageMapping.KEY, key.getId()), //
                        new DBParam(MessageMapping.PAYLOAD, payloadSerializer.serialize(payload)) //
                        );

                for (MessageConsumerRouter<P> consumer : consumers.values())
                {
                    consumer.route(messageMapping.getID(), payload, tags);
                }

                return null;
            }

        });
    }

    /**
     * @see MessagingService#ok(MessageConsumer, int)
     * 
     * @param consumer
     * @param messageId
     */
    public void ok(MessageConsumer<P> consumer, int messageId)
    {
        consumers.get(consumer.getId()).ok(messageId);
    }

    /**
     * @see MessagingService#fail(MessageConsumer, int)
     * 
     * @param consumer
     * @param messageId
     */
    public void fail(MessageConsumer<P> consumer, int messageId)
    {
        consumers.get(consumer.getId()).fail(messageId);
    }

    /**
     * @param tag
     *            message discriminator
     * @return Count of queued messages for provided message tag.
     */
    public int getQueuedCount(String tag)
    {
        int result = 0;
        for (MessageConsumerRouter<P> consumer : consumers.values())
        {
            result = Math.max(result, consumer.getQueuedCount(tag));
        }

        return result;
    }

}
