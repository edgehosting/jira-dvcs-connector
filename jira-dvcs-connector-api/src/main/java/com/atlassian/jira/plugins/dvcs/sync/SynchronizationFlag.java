package com.atlassian.jira.plugins.dvcs.sync;

/**
 * Defines type of synchronization.
 */
public enum SynchronizationFlag {

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
    SYNC_PULL_REQUESTS //

}