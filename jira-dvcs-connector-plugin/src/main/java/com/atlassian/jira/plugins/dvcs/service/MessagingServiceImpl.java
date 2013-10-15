package com.atlassian.jira.plugins.dvcs.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.dao.MessageDao;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.service.message.HasProgress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessageKey;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;

/**
 * A {@link MessagingService} implementation.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class MessagingServiceImpl implements MessagingService
{

    /**
     * Injected {@link ActiveObjects} dependency.
     */
    @Resource
    private ActiveObjects activeObjects;

    /**
     * Injected {@link MessageDao} dependency.
     */
    @Resource
    private MessageDao messageDao;

    /**
     * Injected {@link MessageConsumer}-s dependency.
     */
    @Resource
    private MessageConsumer<?>[] consumers;

    /**
     * Injected {@link MessageExecutor} dependency.
     */
    @Resource
    private MessageExecutor messageExecutor;

    /**
     * Maps identity of message key to appropriate message key.
     */
    private final Map<String, MessageKey<?>> idToMessageKey = new ConcurrentHashMap<String, MessageKey<?>>();

    /**
     * {@inheritDoc}
     */
    @Override
    public <P extends HasProgress> void publish(MessageKey<P> key, P payload, String... tags)
    {
        Message<P> message = new Message<P>();
        message.setKey(key);
        message.setPayload(payload);
        message.setPayloadType(key.getPayloadType());
        message.setTags(tags);
        messageDao.save(message);

        messageExecutor.notify(key);
    }

    /**
     * @in
     * @param consumer
     * @param messageId
     */
    @Override
    public <P extends HasProgress> void ok(Message<P> message, MessageConsumer<P> consumer)
    {
        messageDao.markOk(message, consumer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <P extends HasProgress> void fail(Message<P> message, MessageConsumer<P> consumer)
    {
        messageDao.markFail(message, consumer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K extends MessageKey<P>, P extends HasProgress> int getQueuedCount(K key, String tag)
    {
        return messageDao.getMessagesForConsumingCount(key.getId(), tag);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <P extends HasProgress> MessageKey<P> get(final Class<P> payloadType, final String id)
    {
        MessageKey<P> result;

        synchronized (idToMessageKey)
        {
            result = (MessageKey<P>) idToMessageKey.get(id);
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
