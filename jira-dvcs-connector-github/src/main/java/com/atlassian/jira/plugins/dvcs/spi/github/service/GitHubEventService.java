package com.atlassian.jira.plugins.dvcs.spi.github.service;

import com.atlassian.jira.plugins.dvcs.model.Repository;

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
     * 
     * @param repository
     *            for which one
     * @param isSoftSync
     *            is current synchronization soft?
     * @param synchronizationTags
     *            synchronization tags which are used by synchronization over messaging
     */
    void synchronize(Repository repository, boolean isSoftSync, String[] synchronizationTags);

}
