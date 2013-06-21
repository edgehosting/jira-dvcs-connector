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

    /**
     * Constructor.
     * 
     * @param id
     *            {@link #getId()}
     * @param payload
     *            {@link #getPayload()}
     * @param tags
     *            {@link #getTags()}
     */
    public Message(Integer id, P payload, String[] tags)
    {
        this.id = id;
        this.payload = payload;
        this.tags = tags;
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

}