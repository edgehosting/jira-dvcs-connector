package com.atlassian.jira.plugins.dvcs.event;

import javax.annotation.Nonnull;

/**
 * Helper object for capturing/storing events produced during repository synchronisation.
 */
public interface RepositorySync
{
    /**
     * Stores all events captured during this sync.
     *
     * @return this
     */
    @Nonnull
    RepositorySync storeEvents();

    /**
     * Destroys this RepositorySync instance.
     */
    void finishSync();
}
