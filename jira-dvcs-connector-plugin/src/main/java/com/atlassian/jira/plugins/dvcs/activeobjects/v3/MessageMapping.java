package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import com.atlassian.jira.plugins.dvcs.model.Message;
import net.java.ao.Entity;
import net.java.ao.OneToMany;
import net.java.ao.Preload;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

/**
 * DB version of {@link Message}
 *
 * @author Stanislav Dvorscak
 *
 */
@Preload
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
    @NotNull
    @Indexed
    String getAddress();

    /**
     * @param address
     *            {@link #getAddress()}
     */
    void setAddress(String address);

    /**
     * @return Priority of message.
     */
    @NotNull
    int getPriority();

    /**
     * @param priority
     *            {@link #getPriority()}
     */
    void setPriority(int priority);

    /**
     * @return Type of payload.
     */
    @NotNull
    String getPayloadType();

    /**
     * @param payloadType
     *            {@link #getPayloadType()}
     */
    void setPayloadType(String payloadType);

    /**
     * @return {@link Message#getPayload()}
     */
    @NotNull
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
    @OneToMany(reverse = "getMessage")
    MessageTagMapping[] getTags();

    /**
     * @return remaining consumers of this message
     */
    @OneToMany(reverse = "getMessage")
    MessageQueueItemMapping[] getQueuesItems();

}
