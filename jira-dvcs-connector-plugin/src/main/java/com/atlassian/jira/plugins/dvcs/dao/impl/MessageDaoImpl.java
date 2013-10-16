package com.atlassian.jira.plugins.dvcs.dao.impl;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Resource;

import net.java.ao.DBParam;
import net.java.ao.EntityStreamCallback;
import net.java.ao.Query;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageQueueItemMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageTagMapping;
import com.atlassian.jira.plugins.dvcs.dao.MessageDao;
import com.atlassian.jira.plugins.dvcs.dao.StreamCallback;
import com.atlassian.jira.plugins.dvcs.util.ao.QueryTemplate;
import com.atlassian.jira.util.collect.MapBuilder;
import com.atlassian.sal.api.transaction.TransactionCallback;

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
     * {@inheritDoc}
     */
    @Override
    public MessageMapping create(final Map<String, Object> message, final String[] tags)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<MessageMapping>()
        {

            @Override
            public MessageMapping doInTransaction()
            {
                MessageMapping result = activeObjects.create(MessageMapping.class, message);
                for (String tag : tags)
                {
                    activeObjects.create(MessageTagMapping.class, //
                            new DBParam(MessageTagMapping.MESSAGE, result.getID()), //
                            new DBParam(MessageTagMapping.TAG, tag) //
                            );
                }
                return result;
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final MessageMapping message)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                for (MessageTagMapping tag : message.getTags()) {
                    activeObjects.delete(tag);
                }
                activeObjects.delete(message);
                return null;
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageMapping getById(int id)
    {
        return activeObjects.get(MessageMapping.class, id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getByTag(String tag, final StreamCallback<MessageMapping> messagesStream)
    {
        activeObjects.stream(MessageMapping.class, new QueryTemplate()
        {

            @Override
            protected void build()
            {
                alias(MessageMapping.class, "message");
                alias(MessageTagMapping.class, "messageTag");

                join(MessageTagMapping.class, column(MessageMapping.class, "ID"), MessageTagMapping.MESSAGE);

                where(eq(column(MessageTagMapping.class, MessageTagMapping.TAG), parameter("tag")));
            }

        }.toQuery(Collections.<String, Object> singletonMap("tag", tag)), new EntityStreamCallback<MessageMapping, Integer>()
        {

            @Override
            public void onRowRead(MessageMapping message)
            {
                messagesStream.callback(message);
            }

        });

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMessagesForConsumingCount(String address, String tag)
    {
        Query query = new QueryTemplate()
        {

            @Override
            protected void build()
            {
                alias(MessageMapping.class, "message");
                alias(MessageTagMapping.class, "messageTag");
                alias(MessageQueueItemMapping.class, "messageQueueItem");

                join(MessageTagMapping.class, column(MessageMapping.class, "ID"), MessageTagMapping.MESSAGE);
                join(MessageQueueItemMapping.class, column(MessageMapping.class, "ID"), MessageQueueItemMapping.MESSAGE);

                where(and( //
                        eq(column(MessageMapping.class, MessageMapping.ADDRESS), parameter("address")), //
                        eq(column(MessageTagMapping.class, MessageTagMapping.TAG), parameter("tag")) //
                ));
            }

        }.toQuery(MapBuilder.<String, Object> build("address", address, "tag", tag));
        return activeObjects.count(MessageMapping.class, query);
    }
}
