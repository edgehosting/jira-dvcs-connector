package com.atlassian.jira.plugins.dvcs.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
public class MessagingServiceImpl implements MessagingService
{

    private final MessageRouter messageRouter;

    /**
     * Maps identity of message key to appropriate message key.
     */
    private final Map<String, MessageKey<?>> idToMessageKey = new ConcurrentHashMap<String, MessageKey<?>>();

    public MessagingServiceImpl(MessageRouter messageRouter)
    {
        this.messageRouter = messageRouter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <P> void publish(MessageKey<P> key, P payload, String... tags)
    {
        messageRouter.publish(key, payload, tags);
    }

    /**
     * @in
     * @param consumer
     * @param messageId
     */
    @Override
    public void ok(MessageConsumer<?> consumer, int messageId)
    {
        messageRouter.ok(consumer, messageId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fail(MessageConsumer<?> consumer, int messageId)
    {
        messageRouter.fail(consumer, messageId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K extends MessageKey<?>> int getQueuedCount(K key, String tag)
    {
        return messageRouter.getQueuedCount(key, tag);
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

}
