package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

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
     * @see #getPayload()
     */
    String PAYLOAD = "PAYLOAD";

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
     * @return awaited/remaining consumers of this message
     */
    MessageConsumerMapping[] getConsumers();

}
