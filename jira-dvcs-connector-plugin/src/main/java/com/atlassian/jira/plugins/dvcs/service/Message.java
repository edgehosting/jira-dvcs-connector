package com.atlassian.jira.plugins.dvcs.service;


/**
 * Message which should be delivered.
 * 
 * @author Stanislav Dvorscak
 * 
 */
final class Message<K, P>
{

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
     * @param payload
     *            {@link #getPayload()}
     * @param tags
     *            {@link #getTags()}
     */
    public Message(P payload, String[] tags)
    {
        this.payload = payload;
        this.tags = tags;
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