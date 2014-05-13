package com.atlassian.jira.plugins.dvcs.dao;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageQueueItemMapping;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.MessageState;

import java.util.Map;

/**
 * DAO layer related to {@link MessageQueueItemMapping}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface MessageQueueItemDao
{

    /**
     * @param queueItem
     *            to create
     * @return created {@link MessageQueueItemMapping}
     */
    MessageQueueItemMapping create(Map<String, Object> queueItem);

    /**
     * @param messageQueueItem
     *            to save/update
     */
    void save(MessageQueueItemMapping messageQueueItem);

    /**
     * @param messageQueueItem
     *            to delete
     */
    void delete(MessageQueueItemMapping messageQueueItem);

    /**
     * @param id
     *            {@link MessageMapping#getID()}
     * @return founded queue item
     */
    MessageQueueItemMapping[] getByMessageId(int id);

    /**
     * @param queue
     *            name of queue
     * @param messageId
     *            {@link MessageQueueItemDao}
     * @return queue item for provided queue name and message
     */
    MessageQueueItemMapping getByQueueAndMessage(String queue, int messageId);

    /**
     * @param tag
     *            {@link Message#getTags()}
     * @param state
     *            {@link MessageQueueItemMapping#getState()}
     * @param stream
     *            searches for {@link MessageQueueItemMapping}-s by provided tag and state
     */
    void getByTagAndState(String tag, MessageState state, StreamCallback<MessageQueueItemMapping> stream);

    /**
     * Returns all {@link MessageQueueItemMapping}-s for provided state.
     * 
     * @param state
     *            for which state
     * @param stream
     *            founded items
     */
    void getByState(MessageState state, StreamCallback<MessageQueueItemMapping> stream);

    /**
     * @param queue
     *            name of queue
     * @param address
     *            of messages
     * @return next message for consuming or null, if queue is already empty
     */
    MessageQueueItemMapping getNextItemForProcessing(String queue, String address);

}
