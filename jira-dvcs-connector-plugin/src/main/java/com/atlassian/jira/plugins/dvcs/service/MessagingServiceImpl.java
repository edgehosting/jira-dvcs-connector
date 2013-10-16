package com.atlassian.jira.plugins.dvcs.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageQueueItemMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageTagMapping;
import com.atlassian.jira.plugins.dvcs.dao.MessageDao;
import com.atlassian.jira.plugins.dvcs.dao.MessageQueueItemDao;
import com.atlassian.jira.plugins.dvcs.dao.StreamCallback;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.MessageState;
import com.atlassian.jira.plugins.dvcs.service.message.HasProgress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;

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
     * Injected {@link MessageQueueItemDao} dependency.
     */
    @Resource
    private MessageQueueItemDao messageQueueItemDao;

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
    private final Map<String, MessageAddress<?>> idToMessageKey = new ConcurrentHashMap<String, MessageAddress<?>>();

    /**
     * Maps between {@link MessagePayloadSerializer#getPayloadType()} and appropriate {@link MessagePayloadSerializer serializer}.
     */
    private final Map<Class<?>, MessagePayloadSerializer<?>> payloadTypeToPayloadSerializer = new ConcurrentHashMap<Class<?>, MessagePayloadSerializer<?>>();

    /**
     * Injected {@link MessagePayloadSerializer}-s dependency.
     */
    @Resource
    private MessagePayloadSerializer<?>[] payloadSerializers;

    /**
     * Injected {@link MessageConsumer}-s dependency.
     */
    @Resource
    private MessageConsumer<?>[] messageConsumers;

    /**
     * Maps between {@link MessageConsumer#getAddress()} and appropriate {@link MessageConsumer consumers}.
     */
    private final ConcurrentMap<String, List<MessageConsumer<?>>> keyToMessageConsumer = new ConcurrentHashMap<String, List<MessageConsumer<?>>>();

    /**
     * Initializes been.
     */
    @PostConstruct
    public void init()
    {
        for (MessageConsumer<?> messageConsumer : messageConsumers)
        {
            List<MessageConsumer<?>> byKey = keyToMessageConsumer.get(messageConsumer.getAddress().getId());
            if (byKey == null)
            {
                CopyOnWriteArrayList<MessageConsumer<?>> newByKey = new CopyOnWriteArrayList<MessageConsumer<?>>();
                byKey = keyToMessageConsumer.putIfAbsent(messageConsumer.getAddress().getId(), newByKey);
                if (byKey == null)
                {
                    byKey = newByKey;
                }
            }
            byKey.add(messageConsumer);
        }
        for (MessagePayloadSerializer<?> payloadSerializer : payloadSerializers)
        {
            payloadTypeToPayloadSerializer.put(payloadSerializer.getPayloadType(), payloadSerializer);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <P extends HasProgress> void publish(MessageAddress<P> key, P payload, String... tags)
    {
        Message<P> message = new Message<P>();
        message.setAddress(key);
        message.setPayload(payload);
        message.setPayloadType(key.getPayloadType());
        message.setTags(tags);
        MessageMapping messageMapping = messageDao.create(toMessageMap(message), tags);

        @SuppressWarnings({ "rawtypes", "unchecked" })
        List<MessageConsumer<P>> byKey = (List) keyToMessageConsumer.get(message.getAddress().getId());
        for (MessageConsumer<P> consumer : byKey)
        {
            messageQueueItemDao.create(messageQueueItemToMap(messageMapping.getID(), consumer.getQueue()));
        }

        messageExecutor.notify(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void pause(String tag)
    {
        messageDao.getByTag(tag, new StreamCallback<MessageMapping>()
        {

            @Override
            public void callback(final MessageMapping message)
            {
                activeObjects.executeInTransaction(new TransactionCallback<Void>()
                {

                    @Override
                    public Void doInTransaction()
                    {
                        for (MessageQueueItemMapping messageQueueItem : messageQueueItemDao.getByMessageId(message.getID()))
                        {
                            // messages, which are running can not be paused!
                            if (!MessageState.RUNNING.equals(messageQueueItem.getState()))
                            {
                                messageQueueItem.setState(MessageState.SLEEPING.name());
                                messageQueueItemDao.save(messageQueueItem);
                            }
                        }
                        return null;
                    }

                });
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resume(String tag)
    {
        messageDao.getByTag(tag, new StreamCallback<MessageMapping>()
        {

            @Override
            public void callback(final MessageMapping message)
            {
                activeObjects.executeInTransaction(new TransactionCallback<Void>()
                {

                    @Override
                    public Void doInTransaction()
                    {
                        for (MessageQueueItemMapping messageQueueItem : messageQueueItemDao.getByMessageId(message.getID()))
                        {
                            messageQueueItem.setState(MessageState.PENDING.name());
                            messageQueueItemDao.save(messageQueueItem);
                        }
                        return null;
                    }

                });
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancel(String tag)
    {
        messageDao.getByTag(tag, new StreamCallback<MessageMapping>()
        {

            @Override
            public void callback(final MessageMapping message)
            {
                activeObjects.executeInTransaction(new TransactionCallback<Void>()
                {

                    @Override
                    public Void doInTransaction()
                    {
                        for (MessageQueueItemMapping queueItem : message.getQueuesItems())
                        {
                            messageQueueItemDao.delete(queueItem);
                        }
                        messageDao.delete(message);
                        return null;
                    }

                });
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <P extends HasProgress> void queued(MessageConsumer<P> consumer, Message<P> message)
    {
        MessageQueueItemMapping queueItem = messageQueueItemDao.getByQueueAndMessage(consumer.getQueue(), message.getId());
        queueItem.setState(MessageState.RUNNING.name());
        messageQueueItemDao.save(queueItem);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <P extends HasProgress> void ok(MessageConsumer<P> consumer, Message<P> message)
    {
        messageQueueItemDao.delete(messageQueueItemDao.getByQueueAndMessage(consumer.getQueue(), message.getId()));
        MessageMapping messageMapping = messageDao.getById(message.getId());
        if (messageMapping.getQueuesItems().length == 0)
        {
            messageDao.delete(messageMapping);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <P extends HasProgress> void fail(MessageConsumer<P> consumer, Message<P> message)
    {
        MessageQueueItemMapping queueItem = messageQueueItemDao.getByQueueAndMessage(consumer.getQueue(), message.getId());
        queueItem.setRetriesCount(queueItem.getRetriesCount() + 1);
        queueItem.setState(MessageState.WAITING_FOR_RETRY.name());
        messageQueueItemDao.save(queueItem);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <P extends HasProgress> Message<P> getNextMessageForConsuming(MessageConsumer<P> consumer, String key)
    {
        MessageQueueItemMapping messageQueueItem = messageQueueItemDao.getNextItemForProcessing(consumer.getQueue(), key);
        if (messageQueueItem == null)
        {
            return null;
        }

        Message<P> result = new Message<P>();
        toMessage(result, messageQueueItem.getMessage());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K extends MessageAddress<P>, P extends HasProgress> int getQueuedCount(K key, String tag)
    {
        return messageDao.getMessagesForConsumingCount(key.getId(), tag);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <P extends HasProgress> MessageAddress<P> get(final Class<P> payloadType, final String id)
    {
        MessageAddress<P> result;

        synchronized (idToMessageKey)
        {
            result = (MessageAddress<P>) idToMessageKey.get(id);
            if (result == null)
            {
                idToMessageKey.put(id, result = new MessageAddress<P>()
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

        return (MessageAddress<P>) result;
    }

    /**
     * Re-maps provided {@link Message} to parameters.
     * 
     * @param source
     *            of mapping
     * @return mapped entity
     */
    private <P extends HasProgress> Map<String, Object> toMessageMap(Message<P> source)
    {
        @SuppressWarnings("unchecked")
        MessagePayloadSerializer<P> payloadSerializer = (MessagePayloadSerializer<P>) payloadTypeToPayloadSerializer.get(source
                .getAddress().getPayloadType());

        Map<String, Object> result = new HashMap<String, Object>();

        result.put(MessageMapping.ADDRESS, source.getAddress().getId());
        result.put(MessageMapping.PRIORITY, source.getPriority());
        result.put(MessageMapping.PAYLOAD_TYPE, source.getPayloadType().getCanonicalName());
        result.put(MessageMapping.PAYLOAD, payloadSerializer.serialize(source.getPayload()));

        return result;
    }

    /**
     * Re-maps provided {@link MessageMapping} to {@link Message}.
     * 
     * @param target
     *            of mapping
     * @param source
     *            of mapping
     */
    @SuppressWarnings("unchecked")
    private <P extends HasProgress> void toMessage(Message<P> target, MessageMapping source)
    {
        Class<P> payloadType;
        try
        {
            payloadType = (Class<P>) Class.forName(source.getPayloadType());
        } catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }

        MessagePayloadSerializer<P> payloadSerializer = (MessagePayloadSerializer<P>) payloadTypeToPayloadSerializer.get(payloadType);
        
        int retriesCount = 0;
        for (MessageQueueItemMapping queueItem : source.getQueuesItems()) {
            retriesCount = Math.max(retriesCount, queueItem.getRetriesCount());
        }

        target.setId(source.getID());
        target.setAddress(get(payloadType, source.getAddress()));
        target.setPayload(payloadSerializer.deserialize(source.getPayload()));
        target.setPayloadType(payloadType);
        target.setPriority(source.getPriority());
        target.setTags(Iterables.toArray(Iterables.transform(Arrays.asList(source.getTags()), new Function<MessageTagMapping, String>()
        {

            @Override
            public String apply(MessageTagMapping input)
            {
                return input.getTag();
            }

        }), String.class));
        target.setRetriesCount(retriesCount);
    }

    /**
     * Re-maps provided data to {@link MessageQueueItemMapping} parameters.
     * 
     * @param messageId
     *            {@link Message#getId()}
     * @param queue
     * @return mapped entity
     */
    private Map<String, Object> messageQueueItemToMap(int messageId, String queue)
    {
        Map<String, Object> result = new HashMap<String, Object>();

        result.put(MessageQueueItemMapping.MESSAGE, messageId);
        result.put(MessageQueueItemMapping.QUEUE, queue);
        result.put(MessageQueueItemMapping.STATE, MessageState.PENDING.name());
        result.put(MessageQueueItemMapping.RETRIES_COUNT, 0);

        return result;

    }

}
