package com.atlassian.jira.plugins.dvcs.service.message;

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
     * @param consumer
     *            of message
     * @param messageId
     *            message for marking
     */
    void ok(MessageConsumer<P> consumer, int messageId);

    /**
     * Marks message specified by provided message id, as proceed successfully.
     * 
     * @param consumer
     *            of message
     * @param messageId
     *            message for marking
     */
    void fail(MessageConsumer<P> consumer, int messageId);

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
