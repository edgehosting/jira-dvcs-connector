package com.atlassian.jira.plugins.dvcs.dao;

import java.util.Map;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageQueueItemMapping;

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
     * @param queue
     *            name of queue
     * @param address
     *            of messages
     * @return next message for consuming or null, if queue is already empty
     */
    MessageQueueItemMapping getNextItemForProcessing(String queue, String address);

}
