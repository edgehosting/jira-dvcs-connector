package com.atlassian.jira.plugins.dvcs.sync;

import java.util.EnumSet;

/**
 * Defines type of synchronization.
 */
public enum SynchronizationFlag
{

    /**
     * Performs soft synchronization instead of full synchronization.
     */
    SOFT_SYNC, //

    /**
     * Performs change-sets, synchronization.
     */
    SYNC_CHANGESETS, //

    /**
     * Performs pull request synchronization.
     */
    SYNC_PULL_REQUESTS, //

    WEBHOOK_SYNC;

    /**
     * No synchronisation flags.
     */
    public static EnumSet<SynchronizationFlag> NO_FLAGS = EnumSet.noneOf(SynchronizationFlag.class);
}
