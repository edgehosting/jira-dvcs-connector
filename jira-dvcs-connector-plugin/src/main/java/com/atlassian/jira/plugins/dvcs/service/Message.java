package com.atlassian.jira.plugins.dvcs.service;

/**
 * Message which should be delivered.
 * 
 * @author Stanislav Dvorscak
 * 
 */
final class Message<P>
{

    /**
     * @see #getId()
     */
    private final Integer id;

    /**
     * @see #getPayload()
     */
    private final P payload;

    /**
     * @see #getTags()
     */
    private final String[] tags;

    private final int retriesCount;

    /**
     * Constructor.
     * 
     * @param id
     *            {@link #getId()}
     * @param payload
     *            {@link #getPayload()}
     * @param tags
     *            {@link #getTags()}
     * @param retriesCount 
     */
    public Message(Integer id, P payload, String[] tags, int retriesCount)
    {
        this.id = id;
        this.payload = payload;
        this.tags = tags;
        this.retriesCount = retriesCount;
    }

    /**
     * @return Identity of message.
     */
    public Integer getId()
    {
        return id;
    }

    /**
     * @return message payload
     */
    public P getPayload()
    {
        return payload;
    }

    /**
     * @return tags of message
     */
    public String[] getTags()
    {
        return tags;
    }

    public int getRetriesCount()
    {
        return retriesCount;
    }

    
}