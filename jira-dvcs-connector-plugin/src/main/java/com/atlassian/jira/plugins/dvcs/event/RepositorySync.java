package com.atlassian.jira.plugins.dvcs.event;

/**
 * Helper object for capturing/storing events produced during repository synchronisation.
 */
public interface RepositorySync
{
    /**
     * Stores all captured events and destroys this RepositorySync instance. Calling this method will guarantee that
     * the sync stops listening to thread events.
     */
    void finish();
}
