package com.atlassian.jira.plugins.dvcs.ondemand;

public interface BitbucketAccountsReloadJobScheduler
{
    /**
     * Schedules the reloading of Bitbucket accounts.
     */
    void schedule();
}
