package com.atlassian.jira.plugins.dvcs.sync;


/**
 * Captures sync events <b>that take place on the current thread</b> and dispatches them to all available {@link
 * SyncEventListener} instances.
 */
public interface SyncThreadEvents
{
    /**
     * Starts listening to ActiveObjects events <b>on the current thread</b>.
     *
     * @return a SyncEvents
     */
    SyncEvents startCapturing();
}
