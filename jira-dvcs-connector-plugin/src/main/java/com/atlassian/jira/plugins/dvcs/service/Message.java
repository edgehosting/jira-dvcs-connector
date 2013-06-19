package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.service.message.MessageTag;

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
    private final MessageTag[] tags;

    /**
     * Constructor.
     * 
     * @param payload
     *            {@link #getPayload()}
     * @param tags
     *            {@link #getTags()}
     */
    public Message(P payload, MessageTag[] tags)
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
    public MessageTag[] getTags()
    {
        return tags;
    }

}