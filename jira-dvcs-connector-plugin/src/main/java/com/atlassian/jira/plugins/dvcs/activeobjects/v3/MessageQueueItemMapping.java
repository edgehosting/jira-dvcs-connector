package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import java.util.Date;

import net.java.ao.Entity;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.Table;

/**
 * Holds information about message consumer.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Table("MESSAGE_QUEUE_ITEM")
public interface MessageQueueItemMapping extends Entity
{

    /**
     * @see #getMessage()
     */
    String MESSAGE = "MESSAGE_ID";

    /**
     * @see #getQueue()
     */
    String QUEUE = "QUEUE";

    /**
     * @see #getState()
     */
    String STATE = "STATE";

    /**
     * @see #getLastFailed()
     */
    String LAST_FAILED = "LAST_FAILED";

    /**
     * @see #getRetriesCount()
     */
    String RETRIES_COUNT = "RETRIES_COUNT";

    /**
     * @return Message for consuming.
     */
    @NotNull
    MessageMapping getMessage();

    /**
     * @return Identifier of consumer.
     */
    @NotNull
    String getQueue();

    /**
     * @param queue
     *            {@link #getQueue()}
     */
    void setQueue(String queue);

    /**
     * @return state of message
     */
    @NotNull
    String getState();

    /**
     * @param state
     *            {@link #getState()}
     */
    void setState(String state);

    /**
     * @return Date when last failed happened.
     */
    Date getLastFailed();

    /**
     * @param lastFailed
     *            {@link #getLastFailed()}
     */
    void setLastFailed(Date lastFailed);

    /**
     * @return Counts how many times was attempted to process message.
     */
    @NotNull
    int getRetriesCount();

    /**
     * @param retriesCount
     *            {@link #getRetriesCount()}
     */
    void setRetriesCount(int retriesCount);

}
