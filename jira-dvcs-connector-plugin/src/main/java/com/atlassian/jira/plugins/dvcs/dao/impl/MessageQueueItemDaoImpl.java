package com.atlassian.jira.plugins.dvcs.dao.impl;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Resource;

import net.java.ao.Query;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageQueueItemMapping;
import com.atlassian.jira.plugins.dvcs.dao.MessageQueueItemDao;
import com.atlassian.jira.plugins.dvcs.model.MessageState;
import com.atlassian.jira.plugins.dvcs.util.ao.QueryTemplate;
import com.atlassian.jira.util.collect.MapBuilder;
import com.atlassian.sal.api.transaction.TransactionCallback;

/**
 * {@link MessageQueueItemDao} implementation over AO.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class MessageQueueItemDaoImpl implements MessageQueueItemDao
{

    /**
     * Injected {@link ActiveObjects} dependency.
     */
    @Resource
    private ActiveObjects activeObjects;

    /**
     * Constructor.
     */
    public MessageQueueItemDaoImpl()
    {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageQueueItemMapping create(final Map<String, Object> parameters)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<MessageQueueItemMapping>()
        {

            @Override
            public MessageQueueItemMapping doInTransaction()
            {
                return activeObjects.create(MessageQueueItemMapping.class, parameters);
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final MessageQueueItemMapping messageQueueItem)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                messageQueueItem.save();
                return null;
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final MessageQueueItemMapping messageQueueItem)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                activeObjects.delete(messageQueueItem);
                return null;
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageQueueItemMapping[] getByMessageId(int id)
    {
        Query query = new QueryTemplate()
        {

            @Override
            protected void build()
            {
                alias(MessageQueueItemMapping.class, "queueItem");
                where(eq(column(MessageQueueItemMapping.class, MessageQueueItemMapping.MESSAGE), parameter("messageId")));
            }

        }.toQuery(Collections.<String, Object> singletonMap("messageId", id));
        return activeObjects.find(MessageQueueItemMapping.class, query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageQueueItemMapping getByQueueAndMessage(String queue, int messageId)
    {
        Query query = new QueryTemplate()
        {

            @Override
            protected void build()
            {
                alias(MessageQueueItemMapping.class, "messageQueue");
                where(and( //
                        eq(column(MessageQueueItemMapping.class, MessageQueueItemMapping.QUEUE), parameter("queue")), //
                        eq(column(MessageQueueItemMapping.class, MessageQueueItemMapping.MESSAGE), parameter("messageId")) //
                ));
            }
        }.toQuery(MapBuilder.<String, Object> build("queue", queue, "messageId", messageId));

        MessageQueueItemMapping[] founded = activeObjects.find(MessageQueueItemMapping.class, query);
        return founded.length == 1 ? founded[0] : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageQueueItemMapping getNextItemForProcessing(String queue, String address)
    {
        Query query = new QueryTemplate()
        {

            @Override
            protected void build()
            {
                alias(MessageQueueItemMapping.class, "queueItem");
                alias(MessageMapping.class, "message");

                join(MessageMapping.class, column(MessageQueueItemMapping.class, MessageQueueItemMapping.MESSAGE), "ID");

                where(and(//
                        eq(column(MessageMapping.class, MessageMapping.ADDRESS), parameter("address")), //
                        eq(column(MessageQueueItemMapping.class, MessageQueueItemMapping.QUEUE), parameter("queue")), //
                        eq(column(MessageQueueItemMapping.class, MessageQueueItemMapping.STATE), parameter("state")) //
                ));

                order(orderBy(column(MessageMapping.class, MessageMapping.PRIORITY), false));
            }

        }.toQuery(MapBuilder.<String, Object> build("address", address, "queue", queue, "state", MessageState.PENDING));
        query.limit(1);

        MessageQueueItemMapping[] founded = activeObjects.find(MessageQueueItemMapping.class, query);
        return founded.length == 1 ? founded[0] : null;
    }

}
