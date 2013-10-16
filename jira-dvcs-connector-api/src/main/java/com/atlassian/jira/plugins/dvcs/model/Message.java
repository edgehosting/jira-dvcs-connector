package com.atlassian.jira.plugins.dvcs.model;

import com.atlassian.jira.plugins.dvcs.service.message.HasProgress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageKey;

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
     * @see #getKey()
     */
    private MessageKey<P> key;

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
    private P payload;

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
    public MessageKey<P> getKey()
    {
        return key;
    }

    /**
     * @param key
     *            {@link #getKey()}
     */
    public void setKey(MessageKey<P> key)
    {
        this.key = key;
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
    public P getPayload()
    {
        return payload;
    }

    /**
     * @param payload
     *            {@link #getPayload()}
     */
    public void setPayload(P payload)
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