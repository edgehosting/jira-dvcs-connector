package com.atlassian.jira.plugins.dvcs.service.message;

import com.atlassian.jira.plugins.dvcs.model.DiscardReason;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.MessageState;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;

/**
 * Provides services related to messaging.
 *
 * @author Stanislav Dvorscak
 *
 */
public interface MessagingService
{
    public static final int DEFAULT_PRIORITY = 0;
    public static final int SOFTSYNC_PRIORITY = 10;

    /**
     * Publishes a message with provided payload under provided address.
     *
     * @param address
     *            for publication
     * @param payload
     *            for publication
     * @param tags
     *            of messages
     */
    <P extends HasProgress> void publish(MessageAddress<P> address, P payload, String... tags);

    /**
     * Publishes a message with provided payload under provided address.
     *
     * @param address
     *            for publication
     * @param payload
     *            for publication
     * @param priority
     *            priority of message
     * @param tags
     *            of messages
     */
    <P extends HasProgress> void publish(MessageAddress<P> address, P payload, int priority, String... tags);

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
     * Retries all messages, which are marked by provided tag, and are in {@link MessageState#WAITING_FOR_RETRY}.
     * 
     * @param tag
     *            {@link Message#getTags()}
     * @param auditId
     *            ID for audit log {@link #getTagForAuditSynchronization(int)}
     */
    void retry(String tag, int auditId);

    /**
     * Cancels all messages, which are marked by provided tag.
     *
     * @param tag
     *            {@link Message#getTags()}
     */
    void cancel(String tag);

    /**
     * Marks provided message as running / in progress.
     *
     * @param consumer
     *            owner of processing
     * @param message
     *            for makring
     */
    <P extends HasProgress> void running(MessageConsumer<P> consumer, Message<P> message);

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
    <P extends HasProgress> void fail(MessageConsumer<P> consumer, Message<P> message, Throwable t);

    /**
     * Discards message.
     *
     * @param consumer
     *            of message
     * @param message
     * @param discardReason
     */
    <P extends HasProgress> void discard(final MessageConsumer<P> consumer, Message<P> message, final DiscardReason discardReason);

    /**
     * Get the next message to consume.
     *
     * @param consumer {@link MessageConsumer#getQueue()}
     * @param address {@link Message#getAddress()}
     * @param <P> The type of message we are processing
     *
     * @return next message for consuming or null, if queue is already empty
     */
    public <P extends HasProgress> Message<P> getNextMessageForConsuming(MessageConsumer<P> consumer, String address);

    /**
     * Returns count of queued messages with provided publication address and marked by provided tag.
     *
     * @param tag
     *            of message
     * @return count of queued messages
     */
    int getQueuedCount(String tag);

    /**
     * Creates message address, necessary by publishing and routing.
     *
     * @param payloadType
     *            type of payload
     * @param id
     *            of route
     * @return created message address
     */
    <P extends HasProgress> MessageAddress<P> get(Class<P> payloadType, String id);

    /**
     * @param repository
     * @return message tag for a synchronization
     */
    String getTagForSynchronization(Repository repository);

    String getTagForAuditSynchronization(int id);

    /**
     * Extracts repository from message
     *
     * @param message
     * @return repository
     */
    <P extends HasProgress> Repository getRepositoryFromMessage(Message<P> message);

    /**
     * Extracts id for synchronization audit from provided message.
     * 
     * @param tags
     *            for processing - {@link Message#getTags()}
     * @return synchronization audit id or 0 if does not exists
     */
    int getSynchronizationAuditIdFromTags(String[] tags);

    /**
     * Ends progress if no messages left for repository. This includes kicking off smart commits for the repository if
     * necessary and dispatching any synchronisation-related events.
     *
     * @param repo
     * @param progress
     * @param consumer
     */
    <P extends HasProgress> void tryEndProgress(Repository repo, Progress progress, MessageConsumer<P> consumer, int auditId);

    <P extends  HasProgress> P deserializePayload(Message<P> message);

    void onStart();
}
