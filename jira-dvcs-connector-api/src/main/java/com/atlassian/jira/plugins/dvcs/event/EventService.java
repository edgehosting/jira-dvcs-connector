package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.plugins.dvcs.model.Repository;

/**
 * Service for managing synchronisation events.
 */
public interface EventService
{
    /**
     * Stores the given sync event object against a repository. This method uses Jackson to serialise {@code event} into
     * JSON.
     * <p/>
     * <p/>
     * This method delegates to {@link #storeEvent(com.atlassian.jira.plugins.dvcs.model.Repository, SyncEvent,
     * boolean)} passing {@code scheduled=false}.
     *
     * @param repository the Repository against which to store the event
     * @param event the event to save
     * @throws java.lang.IllegalArgumentException if Jackson is not able to serialise {@code event}
     * @deprecated Use {@link #storeEvent(com.atlassian.jira.plugins.dvcs.model.Repository, SyncEvent, boolean)}
     * instead.
     */
    @Deprecated
    void storeEvent(Repository repository, SyncEvent event) throws IllegalArgumentException;

    /**
     * Stores the given sync event object against a repository. This method uses Jackson to serialise {@code event} into
     * JSON.
     * <p/>
     * The {@code scheduled} parameter governs how event limits are applied when dispatching this event (limits are
     * higher for event raised during a scheduled sync).
     *
     * @param repository the Repository against which to store the event
     * @param event the event to save
     * @param scheduled whether this event was raised during a scheduled sync
     * @throws java.lang.IllegalArgumentException if Jackson is not able to serialise {@code event}
     */
    void storeEvent(Repository repository, SyncEvent event, boolean scheduled) throws IllegalArgumentException;

    /**
     * Stores the event against the repository, see {@link #storeEvent(com.atlassian.jira.plugins.dvcs.model.Repository,
     * SyncEvent, boolean)}
     */
    void storeEvent(int repositoryId, SyncEvent event, boolean scheduled) throws IllegalArgumentException;

    /**
     * Dispatches all pending events for the given Repository on the JIRA EventPublisher. This method deletes events as
     * they are published. Note that this method <b>schedules dispatching for asynchronously execution</b> and returns
     * immediately.
     * <p/>
     * Synchronisation events may be subjected to limits. See {@link com.atlassian.jira.plugins.dvcs.event.EventLimit}.
     *
     * @param repository a Repository
     * @throws java.util.concurrent.RejectedExecutionException if dispatching can not be scheduled for execution
     */
    void dispatchEvents(Repository repository);

    /**
     * Dispatch all events for the supplied repository synchronously, this method will block until all events are
     * published.
     */
    void dispatchEvents(int repositoryId);

    /**
     * Discards all pending events for the given Repository.
     *
     * @param repository a Repository
     */
    void discardEvents(Repository repository);
}
