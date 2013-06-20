package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import java.util.Date;

import net.java.ao.Entity;
import net.java.ao.ManyToMany;
import net.java.ao.schema.Table;

import com.atlassian.sal.api.message.Message;

/**
 * DB version of {@link Message}
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Table("MESSAGE")
public interface MessageMapping extends Entity
{

    /**
     * @see #getKey()
     */
    String KEY = "KEY";

    /**
     * @see #getConsumer()
     */
    String CONSUMER = "CONSUMER";

    /**
     * @see #getPayload()
     */
    String PAYLOAD = "PAYLOAD";

    /**
     * @see #getLastFailed()
     */
    String LAST_FAILED = "LAST_FAILED";

    /**
     * @see #getRetriesCount()
     */
    String RETRIES_COUNT = "RETRIES_COUNT";

    /**
     * @return Routing key of message.
     */
    String getKey();

    /**
     * @param key
     *            {@link #getKey()}
     */
    void setKey(String key);

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
     * @return Payload of message.
     */
    String getPayload();

    /**
     * @param payload
     *            {@link #getPayload()}
     */
    void setPayload(String payload);

    /**
     * @return Marker tags of message.
     */
    @ManyToMany(value = MessageTagMapping.class)
    String[] getTags();

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
