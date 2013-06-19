package com.atlassian.jira.plugins.dvcs.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.DisposableBean;

import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessageKey;
import com.atlassian.jira.plugins.dvcs.service.message.MessageTag;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;

/**
 * A {@link MessagingService} implementation.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class MessagingServiceImpl implements MessagingService, DisposableBean
{

    /**
     * Holds key based messages routers.
     */
    private final Map<MessageKey<Object>, MessageKeyRouter<MessageKey<Object>, Object>> messageRouters = new ConcurrentHashMap<MessageKey<Object>, MessageKeyRouter<MessageKey<Object>, Object>>();

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

            consumersByKey.addConsumer(new MessageConsumerRouter<MessageKey<Object>, Object>((MessageConsumer<Object>) consumer));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K extends MessageKey<P>, P> void publish(K key, P payload, MessageTag... tags)
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
    public <K extends MessageKey<?>> int getQueuedCount(K key, MessageTag tag)
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
    @Override
    public void destroy() throws Exception
    {
        for (MessageKeyRouter<?, ?> messagesRouter : messageRouters.values())
        {
            messagesRouter.stop();
        }
    }

}
