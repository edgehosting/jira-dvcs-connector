package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Table;

import java.util.Date;

/**
 * Holds information about message consumer.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Preload
@Table("MESSAGE_CONSUMER")
public interface MessageConsumerMapping extends Entity
{

    /**
     * @see #getMessage()
     */
    String MESSAGE = "MESSAGE_ID";

    /**
     * @see #getConsumer()
     */
    String CONSUMER = "CONSUMER";

    /**
     * @see #isQueued()
     */
    String QUEUED = "QUEUED";

    /**
     * @see #isWaitForRetry()
     */
    String WAIT_FOR_RETRY = "WAIT_FOR_RETRY";

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
    MessageMapping getMessage();

    /**
     * @return Identifier of consumer.
     */
    String getConsumer();

    /**
     * @param consumer
     *            {@link #getConsumer()}
     */
    void setConsumer(String consumer);

    /**
     * @return true if it is waiting for processing - was already queued.
     */
    boolean isQueued();

    /**
     * @param queued
     *            {@link #isQueued()}
     */
    void setQueued(boolean queued);

    /**
     * @return True if this message is waiting for retry.
     */
    boolean isWaitForRetry();

    /**
     * @param waitForRetry
     *            {@link #isWaitForRetry()}
     */
    void setWaitForRetry(boolean waitForRetry);

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
    int getRetriesCount();

    /**
     * @param retriesCount
     *            {@link #getRetriesCount()}
     */
    void setRetriesCount(int retriesCount);

}
