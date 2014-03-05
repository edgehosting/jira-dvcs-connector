package com.atlassian.jira.plugins.dvcs.sync;

/**
 * An object used to control capture of synchronisation-related thread events. Events are captures in this SyncEvents
 * instance until {@link #stopCapturing()} is called.
 */
public interface SyncEvents
{
    /**
     * Publishes all captured sync events.
     */
    void publish();

    /**
     * Stops listening to sync events.
     */
    void stopCapturing();
}
