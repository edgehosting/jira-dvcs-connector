package com.atlassian.jira.plugins.dvcs.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.DisposableBean;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.service.message.HasProgress;
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
public class MessageRouterImpl<P extends HasProgress> implements MessageRouter<P>, DisposableBean
{

    /**
     * Holds key based messages routers.
     */
    private final Map<MessageKey<P>, MessageKeyRouter<P>> keyToMessageRouter = new ConcurrentHashMap<MessageKey<P>, MessageKeyRouter<P>>();

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
    public MessageRouterImpl(ActiveObjects activeObjects, MessageConsumer<P>[] consumers, MessagePayloadSerializer<P>[] payloadSerializers)
    {
        Map<Class<?>, MessagePayloadSerializer<P>> typeToSerializer = new HashMap<Class<?>, MessagePayloadSerializer<P>>();
        for (MessagePayloadSerializer<P> payloadSerializer : payloadSerializers)
        {
            typeToSerializer.put(payloadSerializer.getPayloadType(), payloadSerializer);
        }

        for (MessageConsumer<P> consumer : consumers)
        {
            MessagePayloadSerializer<P> payloadSerializer = typeToSerializer.get(consumer.getKey().getPayloadType());

            MessageKeyRouter<P> consumersByKey;
            synchronized (this.keyToMessageRouter)
            {
                consumersByKey = this.keyToMessageRouter.get(consumer.getKey());
                if (consumersByKey == null)
                {
                    this.keyToMessageRouter.put((MessageKey<P>) consumer.getKey(), consumersByKey = new MessageKeyRouter<P>( //
                            activeObjects, //
                            consumer.getKey(), //
                            payloadSerializer));
                }
            }

            if (payloadSerializer == null)
            {
                throw new RuntimeException("Unable to find appropriate serializer for provided payload type: "
                        + consumer.getKey().getPayloadType());
            }

            consumersByKey.addConsumer(new MessageConsumerRouter<P>( //
                    activeObjects, //
                    (MessageKey<P>) consumer.getKey(), //
                    (MessageConsumer<P>) consumer, //
                    (MessagePayloadSerializer<P>) payloadSerializer) //
                    );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void publish(MessageKey<P> key, P payload, String... tags)
    {
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
    @Override
    public void ok(MessageConsumer<P> consumer, int messageId)
    {
        MessageKeyRouter<P> messageKeyRouter = keyToMessageRouter.get(consumer.getKey());
        if (messageKeyRouter != null)
        {
            messageKeyRouter.ok(consumer, messageId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fail(MessageConsumer<P> consumer, int messageId)
    {
        MessageKeyRouter<P> messageKeyRouter = keyToMessageRouter.get(consumer.getKey());
        if (messageKeyRouter != null)
        {
            messageKeyRouter.fail(consumer, messageId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K extends MessageKey<P>> int getQueuedCount(K key, String tag)
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
