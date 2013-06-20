package com.atlassian.jira.plugins.dvcs.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.DisposableBean;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessageKey;
import com.atlassian.jira.plugins.dvcs.service.message.MessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;

/**
 * A {@link MessagingService} implementation.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class MessagingServiceImpl implements MessagingService, DisposableBean
{

    // ==========================================
    // ===== Injected dependencies
    // ==========================================

    /**
     * @see #setActiveObjects(ActiveObjects)
     */
    private ActiveObjects activeObjects;

    /**
     * Holds key based messages routers.
     */
    private final Map<MessageKey<Object>, MessageKeyRouter<MessageKey<Object>, Object>> messageRouters = new ConcurrentHashMap<MessageKey<Object>, MessageKeyRouter<MessageKey<Object>, Object>>();

    /**
     * Maps identity of message key to appropriate message key.
     */
    private final Map<String, MessageKey<?>> idToMessageKey = new ConcurrentHashMap<String, MessageKey<?>>();

    /**
     * Maps type of payload to appropriate {@link MessagePayloadSerializer}.
     */
    private final Map<Class<?>, MessagePayloadSerializer<?>> payloadTypeToSerializer = new ConcurrentHashMap<Class<?>, MessagePayloadSerializer<?>>();

    /**
     * @param activeObjects
     *            injected {@link ActiveObjects} dependency
     */
    public void setActiveObjects(ActiveObjects activeObjects)
    {
        this.activeObjects = activeObjects;
    }

    /**
     * @param payloadSerializers
     *            injected {@link MessagePayloadSerializer} dependencies
     */
    public void setMessagePayloadSerializer(MessagePayloadSerializer<?>[] payloadSerializers)
    {
        for (MessagePayloadSerializer<?> payloadSerializer : payloadSerializers)
        {
            payloadTypeToSerializer.put(payloadSerializer.getPayloadType(), payloadSerializer);
        }
    }

    // ==========================================
    // ===== End of: Injected dependencies
    // ==========================================

    /**
     * @param consumers
     *            injected {@link MessageConsumer}-s dependencies
     */
    @SuppressWarnings("unchecked")
    public void setConsumers(MessageConsumer<?>[] consumers)
    {
        for (MessageConsumer<?> consumer : consumers)
        {
            MessageKeyRouter<MessageKey<Object>, Object> consumersByKey;
            synchronized (this.messageRouters)
            {
                consumersByKey = (MessageKeyRouter<MessageKey<Object>, Object>) this.messageRouters.get(consumer.getKey());
                if (consumersByKey == null)
                {
                    this.messageRouters.put((MessageKey<Object>) consumer.getKey(),
                            consumersByKey = new MessageKeyRouter<MessageKey<Object>, Object>(consumer.getKey()));
                }
            }

            MessagePayloadSerializer<?> serializer = payloadTypeToSerializer.get(consumer.getKey().getPayloadType());
            if (serializer == null)
            {
                throw new RuntimeException("Unable to find appropriate serializer for provided payload type: "
                        + consumer.getKey().getPayloadType());
            }

            consumersByKey.addConsumer(new MessageConsumerRouter<MessageKey<Object>, Object>( //
                    activeObjects, //
                    (MessageKey<Object>) consumer.getKey(), //
                    (MessageConsumer<Object>) consumer, //
                    (MessagePayloadSerializer<Object>) serializer) //
                    );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K extends MessageKey<P>, P> void publish(K key, P payload, String... tags)
    {
        @SuppressWarnings("unchecked")
        MessageKeyRouter<K, P> messageKeyRouter = (MessageKeyRouter<K, P>) messageRouters.get(key);
        if (messageKeyRouter != null)
        {
            messageKeyRouter.route(new Message<K, P>(payload, tags));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K extends MessageKey<?>> int getQueuedCount(K key, String tag)
    {
        @SuppressWarnings("unchecked")
        MessageKeyRouter<K, ?> messageKeyRouter = (MessageKeyRouter<K, ?>) messageRouters.get(key);
        if (messageKeyRouter != null)
        {
            return messageKeyRouter.getQueuedCount(tag);
        }

        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <P> MessageKey<P> get(final Class<P> payloadType, final String id)
    {
        MessageKey<?> result;

        synchronized (idToMessageKey)
        {
            result = idToMessageKey.get(id);
            if (result == null)
            {
                idToMessageKey.put(id, result = new MessageKey<P>()
                {

                    @Override
                    public String getId()
                    {
                        return id;
                    }

                    @Override
                    public Class<P> getPayloadType()
                    {
                        return payloadType;
                    }

                });
            }
        }

        return (MessageKey<P>) result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() throws Exception
    {
        for (MessageKeyRouter<?, ?> messagesRouter : messageRouters.values())
        {
            messagesRouter.stop();
        }
    }

}
