package com.atlassian.jira.plugins.dvcs.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageQueueItemMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageTagMapping;
import com.atlassian.jira.plugins.dvcs.dao.MessageDao;
import com.atlassian.jira.plugins.dvcs.dao.MessageQueueItemDao;
import com.atlassian.jira.plugins.dvcs.dao.StreamCallback;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.MessageState;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.HasProgress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.plugin.PluginException;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * A {@link MessagingService} implementation.
 *
 * @author Stanislav Dvorscak
 *
 */
public class MessagingServiceImpl implements MessagingService
{

    private static final String SYNCHRONIZATION_REPO_TAG_PREFIX = "synchronization-repository-";

    /**
     * Logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MessagingServiceImpl.class);

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
     * Maps identity of message address to appropriate {@link MessageAddress}.
     */
    private final Map<String, MessageAddress<?>> idToMessageAddress = new ConcurrentHashMap<String, MessageAddress<?>>();

    /**
     * Maps between {@link MessagePayloadSerializer#getPayloadType()} and appropriate {@link MessagePayloadSerializer serializer}.
     */
    private final Map<Class<?>, MessagePayloadSerializer<?>> payloadTypeToPayloadSerializer = new ConcurrentHashMap<Class<?>, MessagePayloadSerializer<?>>();

    /**
     * Maps between {@link MessageConsumer#getQueue()} and appropriate {@link MessageConsumer}.
     */
    private final ConcurrentMap<String, MessageConsumer<?>> queueToMessageConsumer = new ConcurrentHashMap<String, MessageConsumer<?>>();

    /**
     * Maps between {@link MessageConsumer#getAddress()} and appropriate {@link MessageConsumer consumers}.
     */
    private final ConcurrentMap<String, List<MessageConsumer<?>>> addressToMessageConsumer = new ConcurrentHashMap<String, List<MessageConsumer<?>>>();

    /**
     * Contains all tags which are currently paused.
     */
    private final Set<String> pausedTags = new CopyOnWriteArraySet<String>();

    /**
     * Initializes been.
     */
    @PostConstruct
    public void init()
    {
        for (MessageConsumer<?> messageConsumer : messageConsumers)
        {
            queueToMessageConsumer.putIfAbsent(messageConsumer.getQueue(), messageConsumer);
            List<MessageConsumer<?>> byAddress = addressToMessageConsumer.get(messageConsumer.getAddress().getId());
            if (byAddress == null)
            {
                addressToMessageConsumer.putIfAbsent(messageConsumer.getAddress().getId(),
                        byAddress = new CopyOnWriteArrayList<MessageConsumer<?>>());
            }
            byAddress.add(messageConsumer);
        }

        for (MessagePayloadSerializer<?> payloadSerializer : payloadSerializers)
        {
            payloadTypeToPayloadSerializer.put(payloadSerializer.getPayloadType(), payloadSerializer);
        }

        new Thread(new Runnable()
        {

            @Override
            public void run()
            {
                waitForAO();
                initRunningToFail();
                restartConsumers();
            }

        }).start();
    }

    /**
     * Wait until AO is fully accessible.
     */
    private void waitForAO()
    {
        boolean aoInitialized = false;
        do
        {
            try
            {
                LOGGER.debug("Attempting to wait for AO.");
                activeObjects.count(MessageMapping.class);
                aoInitialized = true;
            } catch (PluginException e)
            {
                try
                {
                    Thread.sleep(5000);
                } catch (InterruptedException ie)
                {
                    // nothing to do
                }
            }
        } while (!aoInitialized);
        LOGGER.debug("Attempting to wait for AO - DONE.");
    }

    /**
     * Marks failed all messages, which are in state running
     */
    private void initRunningToFail()
    {
        messageQueueItemDao.getByState(MessageState.RUNNING, new StreamCallback<MessageQueueItemMapping>()
        {

            @Override
            public void callback(MessageQueueItemMapping e)
            {
                Message<HasProgress> message = new Message<HasProgress>();
                @SuppressWarnings("unchecked")
                MessageConsumer<HasProgress> consumer = (MessageConsumer<HasProgress>) queueToMessageConsumer.get(e.getQueue());

                toMessage(message, e.getMessage());
                fail(consumer, message);
            }

        });
    }

    /**
     * Restart consumers.
     */
    private void restartConsumers()
    {
        new Thread(new Runnable()
        {

            @Override
            public void run()
            {
                Set<MessageAddress<?>> allAddresses = Sets.<MessageAddress<?>> newHashSet(Iterables.transform(
                        Arrays.asList(messageConsumers), new Function<MessageConsumer<?>, MessageAddress<?>>()
                        {

                            @Override
                            public MessageAddress<?> apply(MessageConsumer<?> input)
                            {
                                return input.getAddress();
                            }

                        }));

                for (MessageAddress<?> address : allAddresses)
                {
                    messageExecutor.notify(address);
                }
            }

        }).start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <P extends HasProgress> void publish(MessageAddress<P> address, P payload, String... tags)
    {
        MessageState state = MessageState.PENDING;
        for (String tag : tags)
        {
            if (pausedTags.contains(tag))
            {
                state = MessageState.SLEEPING;
                break;
            }
        }

        Message<P> message = new Message<P>();
        message.setAddress(address);
        message.setPayload(payload);
        message.setPayloadType(address.getPayloadType());
        message.setTags(tags);
        MessageMapping messageMapping = messageDao.create(toMessageMap(message), tags);

        @SuppressWarnings({ "rawtypes", "unchecked" })
        List<MessageConsumer<P>> byAddress = (List) addressToMessageConsumer.get(message.getAddress().getId());
        for (MessageConsumer<P> consumer : byAddress)
        {
            messageQueueItemDao.create(messageQueueItemToMap(messageMapping.getID(), consumer.getQueue(), state));
        }

        messageExecutor.notify(address);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void pause(String tag)
    {
        pausedTags.add(tag);
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
                            if (!MessageState.RUNNING.name().equals(messageQueueItem.getState()))
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
        pausedTags.remove(tag);
        final Set<MessageAddress<?>> addresses = new HashSet<MessageAddress<?>>();
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
                            if (MessageState.SLEEPING.name().equals(messageQueueItem.getState()))
                            {
                                messageQueueItem.setState(MessageState.PENDING.name());
                                messageQueueItemDao.save(messageQueueItem);

                                try
                                {
                                    @SuppressWarnings({ "unchecked", "rawtypes" })
                                    MessageAddress messageAddress = get((Class) Class.forName(messageQueueItem.getMessage()
                                            .getPayloadType()), messageQueueItem.getMessage().getAddress());
                                    addresses.add(messageAddress);
                                } catch (ClassNotFoundException e)
                                {
                                    throw new RuntimeException(e);
                                }
                            }
                        }

                        return null;
                    }

                });
            }

        });

        for (MessageAddress<?> address : addresses)
        {
            messageExecutor.notify(address);
        }
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
    public <P extends HasProgress> Message<P> getNextMessageForConsuming(MessageConsumer<P> consumer, String address)
    {
        MessageQueueItemMapping messageQueueItem = messageQueueItemDao.getNextItemForProcessing(consumer.getQueue(), address);
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
    public int getQueuedCount(String tag)
    {
        return messageDao.getMessagesForConsumingCount(tag);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <P extends HasProgress> MessageAddress<P> get(final Class<P> payloadType, final String id)
    {
        MessageAddress<P> result;

        synchronized (idToMessageAddress)
        {
            result = (MessageAddress<P>) idToMessageAddress.get(id);
            if (result == null)
            {
                idToMessageAddress.put(id, result = new MessageAddress<P>()
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
     * {@inheritDoc}
     */
    @Override
    public String getTagForSynchronization(Repository repository)
    {
        return SYNCHRONIZATION_REPO_TAG_PREFIX + repository.getSlug();
    }

    private int repoId(int messageId, MessageTagMapping[] tags)
    {
        for (MessageTagMapping tag : tags)
        {
            if (SYNCHRONIZATION_REPO_TAG_PREFIX.equals(tag.getTag()))
            {
                try
                {
                    return Integer.parseInt(tag.getTag().substring(SYNCHRONIZATION_REPO_TAG_PREFIX.length() + 1));
                } catch (NumberFormatException e)
                {
                    throw new RuntimeException("Can't get repository id from tags for message with ID " + messageId, e);
                }
            }
        }
        throw new RuntimeException("Can't get repository id from tags for message with ID " + messageId);
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
        for (MessageQueueItemMapping queueItem : source.getQueuesItems())
        {
            retriesCount = Math.max(retriesCount, queueItem.getRetriesCount());
        }

        target.setId(source.getID());
        target.setAddress(get(payloadType, source.getAddress()));
        target.setPayload(payloadSerializer.deserialize(source.getID(), source.getPayload(), repoId(source.getID(), source.getTags())));
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
     * @param state
     * @return mapped entity
     */
    private Map<String, Object> messageQueueItemToMap(int messageId, String queue, MessageState state)
    {
        Map<String, Object> result = new HashMap<String, Object>();

        result.put(MessageQueueItemMapping.MESSAGE, messageId);
        result.put(MessageQueueItemMapping.QUEUE, queue);
        result.put(MessageQueueItemMapping.STATE, state.name());
        result.put(MessageQueueItemMapping.RETRIES_COUNT, 0);

        return result;

    }

}
