package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import net.java.ao.Entity;
import net.java.ao.OneToMany;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

import com.atlassian.jira.plugins.dvcs.model.Message;

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
     * @see #getAddress()
     */
    String ADDRESS = "ADDRESS";

    /**
     * @see #getPriority()
     */
    String PRIORITY = "PRIORITY";

    /**
     * @see #getPayloadType()
     */
    String PAYLOAD_TYPE = "PAYLOAD_TYPE";

    /**
     * @see #getPayload()
     */
    String PAYLOAD = "PAYLOAD";

    /**
     * @return {@link Message#getAddress()}
     */
    String getAddress();

    /**
     * @param address
     *            {@link #getAddress()}
     */
    void setAddress(String address);

    /**
     * @return Priority of message.
     */
    int getPriority();

    /**
     * @param priority
     *            {@link #getPriority()}
     */
    void setPriority(int priority);

    /**
     * @return Type of payload.
     */
    String getPayloadType();

    /**
     * @param payloadType
     *            {@link #getPayloadType()}
     */
    void setPayloadType(String payloadType);

    /**
     * @return {@link Message#getPayload()}
     */
    @StringLength(StringLength.UNLIMITED)
    String getPayload();

    /**
     * @param payload
     *            {@link #getPayload()}
     */
    @StringLength(StringLength.UNLIMITED)
    void setPayload(String payload);

    /**
     * @return Marker tags of message.
     */
    @OneToMany()
    MessageTagMapping[] getTags();

    /**
     * @return remaining consumers of this message
     */
    @OneToMany
    MessageQueueItemMapping[] getQueuesItems();

}
