package com.atlassian.jira.plugins.dvcs.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

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
public class MessagingServiceImpl implements MessagingService, InitializingBean, DisposableBean
{

    /**
     * @see #setActiveObjects(ActiveObjects)
     */
    private ActiveObjects activeObjects;

    /**
     * Holds key based messages routers.
     */
    private final Map<MessageKey<Object>, MessageKeyRouter<Object>> keyToMessageRouter = new ConcurrentHashMap<MessageKey<Object>, MessageKeyRouter<Object>>();

    /**
     * Maps identity of message key to appropriate message key.
     */
    private final Map<String, MessageKey<?>> idToMessageKey = new ConcurrentHashMap<String, MessageKey<?>>();

    /**
     * Maps type of payload to appropriate {@link MessagePayloadSerializer}.
     */
    private final Map<Class<?>, MessagePayloadSerializer<?>> payloadTypeToSerializer = new ConcurrentHashMap<Class<?>, MessagePayloadSerializer<?>>();

    /**
     * @see #setConsumers(MessageConsumer[])
     */
    private MessageConsumer<?>[] consumers;

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

    /**
     * @param consumers
     *            injected {@link MessageConsumer}-s dependencies
     */
    public void setConsumers(MessageConsumer<?>[] consumers)
    {
        this.consumers = consumers;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void afterPropertiesSet() throws Exception
    {
        for (MessageConsumer<?> consumer : consumers)
        {
            MessagePayloadSerializer<?> payloadSerializer = payloadTypeToSerializer.get(consumer.getKey().getPayloadType());

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
        for (MessageKeyRouter<?> messagesRouter : keyToMessageRouter.values())
        {
            messagesRouter.stop();
        }
    }

}
