package com.atlassian.jira.plugins.dvcs.service.message;

import com.atlassian.jira.plugins.dvcs.model.Message;

/**
 * Provides services related to messaging.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface MessagingService<P extends HasProgress>
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
    void publish(MessageKey<P> key, P payload, String... tags);

    /**
     * Marks message specified by provided message id, as proceed successfully.
     * 
     * @param message
     *            for marking
     * @param consumer
     *            of message
     */
    void ok(Message<P> message, MessageConsumer<P> consumer);

    /**
     * Marks message specified by provided message id, as proceed successfully.
     * 
     * @param message
     *            for marking
     * @param consumer
     *            of message
     */
    void fail(Message<P> message, MessageConsumer<P> consumer);

    /**
     * Returns count of queued messages with provided publication key and marked by provided tag.
     * 
     * @param key
     *            of message
     * @param tag
     *            of message
     * @return count of queued messages
     */
    <K extends MessageKey<P>> int getQueuedCount(K key, String tag);

    /**
     * Creates message key, necessary by publishing and routing.
     * 
     * @param payloadType
     *            type of payload
     * @param id
     *            of route
     * @return created message key
     */
    MessageKey<P> get(Class<P> payloadType, String id);

}
