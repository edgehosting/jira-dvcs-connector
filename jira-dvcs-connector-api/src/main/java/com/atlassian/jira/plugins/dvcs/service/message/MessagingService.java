package com.atlassian.jira.plugins.dvcs.service.message;

import com.atlassian.jira.plugins.dvcs.model.Message;

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
    <P extends HasProgress> void publish(MessageAddress<P> key, P payload, String... tags);

    /**
     * Pauses all messages, which are marked by provided tag.
     * 
     * @param tag
     *            {@link Message#getTags()}
     */
    void pause(String tag);

    /**
     * Resume all messages, which are marked by provided tag.
     * 
     * @param tag
     *            {@link Message#getTags()}
     */
    void resume(String tag);

    /**
     * Cancels all messages, which are marked by provided tag.
     * 
     * @param tag
     *            {@link Message#getTags()}
     */
    void cancel(String tag);

    <P extends HasProgress> void queued(MessageConsumer<P> consumer, Message<P> message);

    /**
     * Marks message specified by provided message id, as proceed successfully.
     * 
     * @param consumer
     *            of message
     * @param message
     *            for marking
     */
    <P extends HasProgress> void ok(MessageConsumer<P> consumer, Message<P> message);

    /**
     * Marks message specified by provided message id, as proceed successfully.
     * 
     * @param consumer
     *            of message
     * @param message
     *            for marking
     */
    <P extends HasProgress> void fail(MessageConsumer<P> consumer, Message<P> message);

    /**
     * @param key
     *            {@link Message#getAddress()}
     * @param consumerId
     *            {@link MessageConsumer#getQueue()}
     * @return next message for consuming or null, if queue is already empty
     */
    public <P extends HasProgress> Message<P> getNextMessageForConsuming(MessageConsumer<P> consumer, String key);

    /**
     * Returns count of queued messages with provided publication key and marked by provided tag.
     * 
     * @param key
     *            of message
     * @param tag
     *            of message
     * @return count of queued messages
     */
    <K extends MessageAddress<P>, P extends HasProgress> int getQueuedCount(K key, String tag);

    /**
     * Creates message key, necessary by publishing and routing.
     * 
     * @param payloadType
     *            type of payload
     * @param id
     *            of route
     * @return created message key
     */
    <P extends HasProgress> MessageAddress<P> get(Class<P> payloadType, String id);

}
