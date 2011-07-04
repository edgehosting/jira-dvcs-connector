package com.atlassian.jira.plugins.bitbucket.mapper.activeobjects;

/**
 * Active object storage for the progress of a synchronization
 */
public interface SyncProgress
{
    String getProectKey();

    String getRepositoryUri();

    void setProjectKey(String projectKey);

    void setRepositoryUri();
}
