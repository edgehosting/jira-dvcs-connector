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
     *
     * @param repository the Repository against which to store the event
     * @param event the event to save
     * @throws java.lang.IllegalArgumentException if Jackson is not able to serialise {@code event}
     */
    void storeEvent(Repository repository, SyncEvent event) throws IllegalArgumentException;

    /**
     * Dispatches all pending events for the given Repository on the JIRA EventPublisher. This method deletes events as
     * they are published. Note that this method <b>schedules dispatching for asynchronously execution</b> and returns
     * immediately.
     *
     * @param repository a Repository
     * @throws java.util.concurrent.RejectedExecutionException if dispatching can not be scheduled for execution
     */
    void dispatchEvents(Repository repository);

    /**
     * Discards all pending events for the given Repository.
     *
     * @param repository a Repository
     */
    void discardEvents(Repository repository);
}
