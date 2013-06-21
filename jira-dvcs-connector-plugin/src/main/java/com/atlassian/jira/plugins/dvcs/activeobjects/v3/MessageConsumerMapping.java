package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import java.util.Date;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

/**
 * Holds information about message consumer.
 * 
 * @author Stanislav Dvorscak
 * 
 */
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
