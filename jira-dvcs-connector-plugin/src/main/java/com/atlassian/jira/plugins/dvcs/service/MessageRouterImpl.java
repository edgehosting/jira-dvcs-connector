package com.atlassian.jira.plugins.dvcs.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.DisposableBean;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessageKey;
import com.atlassian.jira.plugins.dvcs.service.message.MessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.service.message.MessageRouter;

/**
 * A {@link MessageRouter} implementation.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class MessageRouterImpl implements MessageRouter, DisposableBean
{

    /**
     * Holds key based messages routers.
     */
    private final Map<MessageKey<Object>, MessageKeyRouter<Object>> keyToMessageRouter = new ConcurrentHashMap<MessageKey<Object>, MessageKeyRouter<Object>>();

    /**
     * Constructor.
     * 
     * @param activeObjects
     *            injected {@link ActiveObjects} dependency
     * @param consumers
     *            injected {@link MessageConsumer}-s dependencies
     * @param payloadSerializers
     *            injected {@link MessagePayloadSerializer}-s dependencies
     */
    @SuppressWarnings("unchecked")
    public MessageRouterImpl(ActiveObjects activeObjects, MessageConsumer<?>[] consumers, MessagePayloadSerializer<?>[] payloadSerializers)
    {
        Map<Class<?>, MessagePayloadSerializer<?>> typeToSerializer = new HashMap<Class<?>, MessagePayloadSerializer<?>>();
        for (MessagePayloadSerializer<?> payloadSerializer : payloadSerializers)
        {
            typeToSerializer.put(payloadSerializer.getPayloadType(), payloadSerializer);
        }

        for (MessageConsumer<?> consumer : consumers)
        {
            MessagePayloadSerializer<?> payloadSerializer = typeToSerializer.get(consumer.getKey().getPayloadType());

            MessageKeyRouter<Object> consumersByKey;
            synchronized (this.keyToMessageRouter)
            {
                consumersByKey = (MessageKeyRouter<Object>) this.keyToMessageRouter.get(consumer.getKey());
                if (consumersByKey == null)
                {
                    this.keyToMessageRouter.put((MessageKey<Object>) consumer.getKey(), consumersByKey = new MessageKeyRouter<Object>( //
                            activeObjects, //
                            (MessageKey<Object>) consumer.getKey(), //
                            (MessagePayloadSerializer<Object>) payloadSerializer));
                }
            }

            if (payloadSerializer == null)
            {
                throw new RuntimeException("Unable to find appropriate serializer for provided payload type: "
                        + consumer.getKey().getPayloadType());
            }

            consumersByKey.addConsumer(new MessageConsumerRouter<Object>( //
                    activeObjects, //
                    (MessageKey<Object>) consumer.getKey(), //
                    (MessageConsumer<Object>) consumer, //
                    (MessagePayloadSerializer<Object>) payloadSerializer) //
                    );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <P> void publish(MessageKey<P> key, P payload, String... tags)
    {
        @SuppressWarnings("unchecked")
        MessageKeyRouter<P> messageKeyRouter = (MessageKeyRouter<P>) keyToMessageRouter.get(key);
        if (messageKeyRouter != null)
        {
            messageKeyRouter.route(payload, tags);
        }
    }

    /**
     * @in
     * @param consumer
     * @param messageId
     */
    @SuppressWarnings("unchecked")
    @Override
    public void ok(MessageConsumer<?> consumer, int messageId)
    {
        MessageKeyRouter<Object> messageKeyRouter = (MessageKeyRouter<Object>) keyToMessageRouter.get(consumer.getKey());
        if (messageKeyRouter != null)
        {
            messageKeyRouter.ok((MessageConsumer<Object>) consumer, messageId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void fail(MessageConsumer<?> consumer, int messageId)
    {
        MessageKeyRouter<Object> messageKeyRouter = (MessageKeyRouter<Object>) keyToMessageRouter.get(consumer.getKey());
        if (messageKeyRouter != null)
        {
            messageKeyRouter.fail((MessageConsumer<Object>) consumer, messageId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K extends MessageKey<?>> int getQueuedCount(K key, String tag)
    {
        MessageKeyRouter<?> messageKeyRouter = (MessageKeyRouter<?>) keyToMessageRouter.get(key);
        if (messageKeyRouter != null)
        {
            return messageKeyRouter.getQueuedCount(tag);
        }

        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() throws Exception
    {
        for (MessageKeyRouter<?> messagesRouter : keyToMessageRouter.values())
        {
            messagesRouter.stop();
        }
    }

}
