package com.atlassian.jira.plugins.dvcs.dao.impl;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import net.java.ao.DBParam;
import net.java.ao.Query;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageConsumerMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageTagMapping;
import com.atlassian.jira.plugins.dvcs.dao.MessageDao;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.service.message.HasProgress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagePayloadSerializer;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * An implementation of {@link MessageDao}.
 *
 * @author Stanislav Dvorscak
 *
 */
public class MessageDaoImpl implements MessageDao
{

    /**
     * Injected {@link ActiveObjects} dependency.
     */
    @Resource
    private ActiveObjects activeObjects;

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
     * Maps between {@link MessageConsumer#getKey()} and appropriate {@link MessageConsumer consumers}.
     */
    private final ConcurrentMap<String, List<MessageConsumer<?>>> keyToMessageConsumer = new ConcurrentHashMap<String, List<MessageConsumer<?>>>();

    /**
     * Maps between {@link MessagePayloadSerializer#getPayloadType()} and appropriate {@link MessagePayloadSerializer serializer}.
     */
    private final Map<Class<?>, MessagePayloadSerializer<?>> payloadTypeToPayloadSerializer = new ConcurrentHashMap<Class<?>, MessagePayloadSerializer<?>>();

    private String quoteString = null;

    /**
     * Initializes been.
     */
    @PostConstruct
    public void init()
    {
        for (MessageConsumer<?> messageConsumer : messageConsumers)
        {
            List<MessageConsumer<?>> byKey = keyToMessageConsumer.get(messageConsumer.getKey().getId());
            if (byKey == null)
            {
                CopyOnWriteArrayList<MessageConsumer<?>> newByKey = new CopyOnWriteArrayList<MessageConsumer<?>>();
                byKey = keyToMessageConsumer.putIfAbsent(messageConsumer.getKey().getId(), newByKey);
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
    public <P extends HasProgress> void save(final Message<P> message)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                MessageMapping messageMapping;
                if (message.getId() == null)
                {
                    messageMapping = activeObjects.create(MessageMapping.class, map(message));
                    addToConsumersQueues(messageMapping);

                } else
                {
                    messageMapping = activeObjects.get(MessageMapping.class, message.getId());
                    map(messageMapping, message);
                    messageMapping.save();

                }
                updateMessageTags(messageMapping, message);

                map(message, messageMapping);
                return null;
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <P extends HasProgress> void delete(final Message<P> message)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                MessageMapping messageMapping = activeObjects.get(MessageMapping.class, message.getId());
                for (MessageConsumerMapping consumer : messageMapping.getConsumers())
                {
                    activeObjects.delete(consumer);
                }
                for (MessageTagMapping tag : messageMapping.getTags())
                {
                    activeObjects.delete(tag);
                }
                activeObjects.delete(messageMapping);
                return null;
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <P extends HasProgress> void markQueued(final Message<P> message)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                MessageMapping messageMapping = activeObjects.get(MessageMapping.class, message.getId());
                for (MessageConsumerMapping consumer : messageMapping.getConsumers())
                {
                    if (!consumer.isQueued())
                    {
                        consumer.setQueued(true);
                        consumer.save();
                    }
                }

                return null;
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <P extends HasProgress> void markOk(final Message<P> message, final MessageConsumer<P> consumer)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                Query query = Query
                        .select()
                        .from(MessageConsumerMapping.class)
                        .where(MessageConsumerMapping.MESSAGE + " = ? AND " + MessageConsumerMapping.CONSUMER + " = ?", message.getId(),
                                consumer.getId());
                activeObjects.delete(activeObjects.find(MessageConsumerMapping.class, query));
                MessageMapping messageMapping = activeObjects.get(MessageMapping.class, message.getId());
                if (messageMapping.getConsumers().length == 0)
                {
                    for (MessageTagMapping messageTagMapping : messageMapping.getTags())
                    {
                        activeObjects.delete(messageTagMapping);
                    }
                    activeObjects.delete(messageMapping);
                }

                return null;
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <P extends HasProgress> void markFail(final Message<P> message, final MessageConsumer<P> consumer)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                Query query = Query
                        .select()
                        .from(MessageConsumerMapping.class)
                        .where(MessageConsumerMapping.MESSAGE + " = ? AND " + MessageConsumerMapping.CONSUMER + " = ?", message.getId(),
                                consumer.getId());

                MessageConsumerMapping messageConsumerMapping = activeObjects.find(MessageConsumerMapping.class, query)[0];
                messageConsumerMapping.setLastFailed(new Date());
                messageConsumerMapping.setWaitForRetry(true);
                messageConsumerMapping.setRetriesCount(messageConsumerMapping.getRetriesCount() + 1);
                messageConsumerMapping.save();
                return null;
            }
        });

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <P extends HasProgress> Message<P> getById(int id)
    {
        MessageMapping messageMapping = activeObjects.get(MessageMapping.class, id);
        Message<P> result = new Message<P>();
        map(result, messageMapping);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <P extends HasProgress> Message<P> getNextMessageForConsuming(String key, String consumerId)
    {
        Query query = queryForMessageByConsumer(key, consumerId, "");
        MessageMapping[] found = activeObjects.find(MessageMapping.class, query);
        if (found.length == 0)
        {
            return null;

        } else
        {
            maybeInitQuoteString(found[0]);
            // hack, cause AO incorrectly escapes -> ORDER BY "msg".priority
            query = queryForMessageByConsumer(key, consumerId, " order by msg." + quoteString + MessageMapping.PRIORITY + quoteString + " desc");
            found = activeObjects.find(MessageMapping.class, query);
            return found.length == 0 ? null : map(new Message<P>(), found[0]);
        }
    }

    private void maybeInitQuoteString(MessageMapping entity)
    {
        if (quoteString == null)
        {
            try
            {
                quoteString = entity.getEntityManager().getProvider().getConnection().getMetaData().getIdentifierQuoteString();
            } catch (SQLException e)
            {
                throw new IllegalStateException(e);
            }
        }
    }

    private Query queryForMessageByConsumer(String key, String consumerId, String orderBy)
    {
        return Query.select("ID, " + MessageMapping.PRIORITY).distinct().from(MessageMapping.class)
                .alias(MessageMapping.class, "msg") //
                .join(MessageConsumerMapping.class, "msg.ID = consumer." + MessageConsumerMapping.MESSAGE) //
                .alias(MessageConsumerMapping.class, "consumer") //
                .where( //
                "msg." + MessageMapping.KEY + " = ? AND consumer." + MessageConsumerMapping.CONSUMER + " = ? " //
                        + " AND consumer." + MessageConsumerMapping.QUEUED + " = ?  " //
                        + " AND consumer." + MessageConsumerMapping.WAIT_FOR_RETRY + " = ? " + orderBy, //
                key, consumerId, false, false //
                ).limit(1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMessagesForConsumingCount(String key, String tag)
    {
        Query query = Query
                .select()
                .from(MessageMapping.class)
                .alias(MessageMapping.class, "message")
                .join(MessageConsumerMapping.class, "message.ID = consumer." + MessageConsumerMapping.MESSAGE)
                .join(MessageTagMapping.class, "message.ID = tag." + MessageTagMapping.MESSAGE)
                .alias(MessageConsumerMapping.class, "consumer")
                .alias(MessageTagMapping.class, "tag")
                .where("message." + MessageMapping.KEY + " = ? AND tag.tag = ? AND consumer." + MessageConsumerMapping.WAIT_FOR_RETRY
                        + "  = ?", key, tag, false);
        return activeObjects.count(MessageMapping.class, query);
    }

    /**
     * Re-maps provided {@link MessageMapping} into the {@link Message}.
     *
     * @param target
     *            of mapping
     * @param source
     *            of mapping
     */
    @SuppressWarnings("unchecked")
    private <P extends HasProgress> Message<P> map(Message<P> target, MessageMapping source)
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

        target.setId(source.getID());
        target.setPriority(source.getPriority());
        target.setPayloadType(payloadType);
        target.setPayload(payloadSerializer.deserialize(source.getPayload()));
        target.setTags(Iterables.toArray(
                Iterables.transform(Lists.newArrayList(source.getTags()), new Function<MessageTagMapping, String>()
                {

                    @Override
                    public String apply(MessageTagMapping input)
                    {
                        return input.getTag();
                    }

                }), String.class));
        return target;
    }

    /**
     * Re-maps provided {@link Message} to the {@link DBParam}-s.
     *
     * @param source
     *            of mapping
     * @return mapped entity
     */
    private <P extends HasProgress> DBParam[] map(Message<P> source)
    {
        @SuppressWarnings("unchecked")
        MessagePayloadSerializer<P> payloadSerializer = (MessagePayloadSerializer<P>) payloadTypeToPayloadSerializer.get(source.getKey()
                .getPayloadType());
        return new DBParam[] { //
        //
                new DBParam(MessageMapping.KEY, source.getKey().getId()), //
                new DBParam(MessageMapping.PRIORITY, source.getPriority()), //
                new DBParam(MessageMapping.PAYLOAD_TYPE, source.getPayloadType().getCanonicalName()), //
                new DBParam(MessageMapping.PAYLOAD, payloadSerializer.serialize(source.getPayload())), //
        };
    }

    /**
     * Re-maps provided {@link Message} to the {@link MessageMapping}.
     *
     * @param target
     *            of mapping
     * @param source
     *            of mapping
     */
    private <P extends HasProgress> void map(MessageMapping target, Message<P> source)
    {
        @SuppressWarnings("unchecked")
        MessagePayloadSerializer<P> payloadSerializer = (MessagePayloadSerializer<P>) payloadTypeToPayloadSerializer.get(source.getKey()
                .getPayloadType());
        target.setKey(source.getKey().getId());
        target.setPriority(source.getPriority());
        target.setPayloadType(source.getPayloadType().getCanonicalName());
        target.setPayload(payloadSerializer.serialize(source.getPayload()));
    }

    /**
     * Updates {@link MessageMapping#getTags()} according to current state of {@link Message#getTags()}.
     *
     * @param messageMapping
     *            target of update
     * @param message
     *            source of update
     */
    private <P extends HasProgress> void updateMessageTags(MessageMapping messageMapping, Message<P> message)
    {
        final List<String> currentTags = Arrays.asList(message.getTags());
        final List<String> previousTags = Lists.newLinkedList(Iterables.transform(Arrays.asList(messageMapping.getTags()),
                new Function<MessageTagMapping, String>()
                {

                    @Override
                    public String apply(MessageTagMapping input)
                    {
                        return input.getTag();
                    }

                }));

        Iterable<MessageTagMapping> toRemove = Iterables.filter(Arrays.asList(messageMapping.getTags()), new Predicate<MessageTagMapping>()
        {

            @Override
            public boolean apply(MessageTagMapping input)
            {
                return !currentTags.contains(input.getTag());
            }

        });
        for (MessageTagMapping messageTagMapping : toRemove)
        {
            activeObjects.delete(messageTagMapping);
        }

        Iterable<String> toAdd = Iterables.filter(currentTags, new Predicate<String>()
        {

            @Override
            public boolean apply(String input)
            {
                return !previousTags.contains(input);
            }

        });

        for (String tag : toAdd)
        {
            activeObjects.create(MessageTagMapping.class, //
                    new DBParam(MessageTagMapping.MESSAGE, messageMapping.getID()), //
                    new DBParam(MessageTagMapping.TAG, tag));
        }
    }

    /**
     * Queues provided {@link MessageMapping message} to consumers DB's side queue.
     *
     * @param message
     */
    private <P extends HasProgress> void addToConsumersQueues(MessageMapping message)
    {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        List<MessageConsumer<P>> byKey = (List) keyToMessageConsumer.get(message.getKey());
        for (MessageConsumer<P> consumer : byKey)
        {
            activeObjects.create(MessageConsumerMapping.class, //
                    new DBParam(MessageConsumerMapping.MESSAGE, message.getID()), //
                    new DBParam(MessageConsumerMapping.CONSUMER, consumer.getId()), //
                    new DBParam(MessageConsumerMapping.QUEUED, Boolean.FALSE), //
                    new DBParam(MessageConsumerMapping.WAIT_FOR_RETRY, Boolean.FALSE), //
                    new DBParam(MessageConsumerMapping.RETRIES_COUNT, 0) //
                    );
        }
    }

}
