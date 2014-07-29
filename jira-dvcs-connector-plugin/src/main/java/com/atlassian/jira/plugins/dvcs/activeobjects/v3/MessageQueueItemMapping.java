package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.Table;

import java.util.Date;

/**
 * Holds information about message consumer.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Preload
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
     * @see #getStateInfo()
     */
    String STATE_INFO = "STATE_INFO";

    /**
     * @return Message for consuming.
     */
    @NotNull
    MessageMapping getMessage();
    
    /**
     * @return Identifier of consumer.
     */
    @NotNull
    @Indexed
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
    @Indexed
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

    /**
     *
     * @return info about the state, e.g. the reason for discard state
     */
    String getStateInfo();

    void setStateInfo(String stateInfo);

}
