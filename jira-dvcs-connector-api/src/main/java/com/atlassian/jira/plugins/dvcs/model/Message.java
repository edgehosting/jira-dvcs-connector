package com.atlassian.jira.plugins.dvcs.model;

import com.atlassian.jira.plugins.dvcs.service.message.HasProgress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;

/**
 * Message which should be delivered.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class Message<P extends HasProgress>
{

    /**
     * @see #getId()
     */
    private Integer id;

    /**
     * @see #getAddress()
     */
    private MessageAddress<P> address;

    /**
     * @see #getPriority()
     */
    private int priority;

    /**
     * @see #getPayloadType()
     */
    private Class<P> payloadType;

    /**
     * @see #getPayload()
     */
    private String payload;

    /**
     * @see #getTags()
     */
    private String[] tags = new String[] {};

    /**
     * @see #getRetriesCount()
     */
    private int retriesCount;

    /**
     * @return Identity of message.
     */
    public Integer getId()
    {
        return id;
    }

    /**
     * @return Address where this message should be delivered.
     */
    public MessageAddress<P> getAddress()
    {
        return address;
    }

    /**
     * @param address
     *            {@link #getAddress()}
     */
    public void setAddress(MessageAddress<P> address)
    {
        this.address = address;
    }

    /**
     * @param id
     *            {@link #getId()}
     */
    public void setId(Integer id)
    {
        this.id = id;
    }

    /**
     * @return priority of message
     */
    public int getPriority()
    {
        return priority;
    }

    /**
     * @param priority
     *            {@link #getPriority()}
     */
    public void setPriority(int priority)
    {
        this.priority = priority;
    }

    /**
     * @return type of {@link #getPayload()}
     */
    public Class<P> getPayloadType()
    {
        return payloadType;
    }

    /**
     * @param payloadType
     *            {@link #getPayloadType()}
     */
    public void setPayloadType(Class<P> payloadType)
    {
        this.payloadType = payloadType;
    }

    /**
     * @return message payload
     */
    public String getPayload()
    {
        return payload;
    }

    /**
     * @param payload
     *            {@link #getPayload()}
     */
    public void setPayload(String payload)
    {
        this.payload = payload;
    }

    /**
     * @return tags of message
     */
    public String[] getTags()
    {
        return tags;
    }

    /**
     * @param tags
     *            {@link #getTags()}
     */
    public void setTags(String[] tags)
    {
        this.tags = tags != null ? tags : new String[] {};
    }

    public int getRetriesCount()
    {
        return retriesCount;
    }

    /**
     * @param retriesCount
     *            {@link #getRetriesCount()}
     */
    public void setRetriesCount(int retriesCount)
    {
        this.retriesCount = retriesCount;
    }

}
