package com.atlassian.jira.plugins.dvcs.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;

import com.atlassian.jira.plugins.dvcs.service.message.HasProgress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessageKey;
import com.atlassian.jira.plugins.dvcs.service.message.MessageRouter;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;

/**
 * A {@link MessagingService} implementation.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class MessagingServiceImpl<P extends HasProgress> implements MessagingService<P>
{

    /**
     * Injected {@link MessageRouter} dependency.
     */
    @Resource
    private MessageRouter<P> messageRouter;

    /**
     * Maps identity of message key to appropriate message key.
     */
    private final Map<String, MessageKey<P>> idToMessageKey = new ConcurrentHashMap<String, MessageKey<P>>();

    /**
     * Constructor.
     */
    public MessagingServiceImpl()
    {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void publish(MessageKey<P> key, P payload, String... tags)
    {
        messageRouter.publish(key, payload, tags);
    }

    /**
     * @in
     * @param consumer
     * @param messageId
     */
    @Override
    public void ok(MessageConsumer<P> consumer, int messageId)
    {
        messageRouter.ok(consumer, messageId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fail(MessageConsumer<P> consumer, int messageId)
    {
        messageRouter.fail(consumer, messageId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K extends MessageKey<P>> int getQueuedCount(K key, String tag)
    {
        return messageRouter.getQueuedCount(key, tag);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageKey<P> get(final Class<P> payloadType, final String id)
    {
        MessageKey<P> result;

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

}
