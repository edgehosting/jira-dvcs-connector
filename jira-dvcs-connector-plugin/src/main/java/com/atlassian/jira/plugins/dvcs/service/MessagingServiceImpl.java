package com.atlassian.jira.plugins.dvcs.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
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
public class MessagingServiceImpl<P extends HasProgress> implements MessagingService<P>
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
    private MessageConsumer<P>[] consumers;

    /**
     * Maps identity of message key to appropriate message key.
     */
    private final Map<String, MessageKey<P>> idToMessageKey = new ConcurrentHashMap<String, MessageKey<P>>();

    /**
     * Holds key based messages routers.
     */
    private final ConcurrentMap<MessageKey<P>, Map<String, MessageConsumerRouter<P>>> keyToConsumerIdToMessageConsumerRouter = new ConcurrentHashMap<MessageKey<P>, Map<String, MessageConsumerRouter<P>>>();

    /**
     * Initializes this bean.
     */
    @PostConstruct
    public void init()
    {
        for (MessageConsumer<P> consumer : consumers)
        {
            Map<String, MessageConsumerRouter<P>> byKey = keyToConsumerIdToMessageConsumerRouter.get(consumer.getKey());
            if (byKey == null)
            {
                keyToConsumerIdToMessageConsumerRouter.put(consumer.getKey(),
                        byKey = new ConcurrentHashMap<String, MessageConsumerRouter<P>>());
            }

            byKey.put(consumer.getId(), new MessageConsumerRouter<P>( //
                    activeObjects, //
                    messageDao, //
                    (MessageKey<P>) consumer.getKey(), //
                    (MessageConsumer<P>) consumer //
                    ));
        }
    }

    /**
     * Destroys this bean.
     * 
     * @throws Exception
     */
    @PreDestroy
    public void destroy() throws Exception
    {
        for (Map<String, MessageConsumerRouter<P>> messageRouters : keyToConsumerIdToMessageConsumerRouter.values())
        {
            for (MessageConsumerRouter<P> messageRouter : messageRouters.values())
            {
                messageRouter.stop();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void publish(MessageKey<P> key, P payload, String... tags)
    {
        Message<P> message = new Message<P>();
        message.setKey(key);
        message.setPayload(payload);
        message.setPayloadType(key.getPayloadType());
        message.setTags(tags);
        messageDao.save(message);

        for (MessageConsumerRouter<P> consumer : keyToConsumerIdToMessageConsumerRouter.get(message.getKey()).values())
        {
            consumer.route(message);
        }
    }

    /**
     * @in
     * @param consumer
     * @param messageId
     */
    @Override
    public void ok(Message<P> message, MessageConsumer<P> consumer)
    {
        messageDao.markOk(message, consumer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fail(Message<P> message, MessageConsumer<P> consumer)
    {
        messageDao.markFail(message, consumer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K extends MessageKey<P>> int getQueuedCount(K key, String tag)
    {
        return messageDao.getMessagesForConsumingCount(key.getId(), tag);
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
