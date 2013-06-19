package com.atlassian.jira.plugins.dvcs.service.message;

/**
 * Provides services related to messaging.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface MessagingService
{

    /**
     * Publishes a message with provided payload under provided key.
     * 
     * @param key
     *            for publication
     * @param payload
     *            for publication
     * @param tags
     *            of messages
     */
    <K extends MessageKey<P>, P> void publish(K key, P payload, MessageTag... tags);

    /**
     * Returns count of queued messages with provided publication key and marked by provided tag.
     * 
     * @param key
     *            of message
     * @param tag
     *            of message
     * @return count of queued messages
     */
    <K extends MessageKey<?>> int getQueuedCount(K key, MessageTag tag);

}
