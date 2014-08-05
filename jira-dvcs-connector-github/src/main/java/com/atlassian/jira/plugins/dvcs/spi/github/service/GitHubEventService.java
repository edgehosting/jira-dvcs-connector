package com.atlassian.jira.plugins.dvcs.spi.github.service;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import org.eclipse.egit.github.core.event.Event;

/**
 * Business layer related to GitHub events.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface GitHubEventService
{
    /**
     * Removes all events for provided repository.
     * 
     * @param repository
     *            for which repository
     */
    void removeAll(Repository repository);

    /**
     * Synchronizes all events for a provided repository.
     *  @param repository
     *            for which one
     * @param isSoftSync
     *            is current synchronization soft?
     * @param synchronizationTags
     * @param webHookSync
     */
    void synchronize(Repository repository, boolean isSoftSync, String[] synchronizationTags, boolean webHookSync);

    /**
     * Stores provided {@link Event} locally. If the event with the same id exists, then it is not saved again.
     *
     * @param repository
     *            over of event
     * @param event
     *            GitHub event which was proceed
     * @param savePoint
     *            true if it is save point, false otherwise
     */
    void saveEvent(Repository repository, Event event, boolean savePoint);
}
