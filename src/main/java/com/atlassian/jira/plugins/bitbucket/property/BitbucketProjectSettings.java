package com.atlassian.jira.plugins.bitbucket.property;

import java.util.List;

/**
 * Access to the settings for this plugins state
 */
public interface BitbucketProjectSettings
{
    BitbucketSyncProgress getSyncProgress(String projectKey, String repositoryUrl);

    void startSyncProgress(String projectKey, String repositoryUrl);
    void setSyncProgress(String projectKey, String repositoryUrl, int revision);
    void completeSyncProgress(String projectKey, String repositoryUrl);

    int getCount(String projectKey, String repositoryUrl, String type);
    void incrementCommitCount(String projectKey, String repositoryURL, String type);

    String getUsername(String projectKey, String repositoryURL);
    String getPassword(String projectKey, String repositoryURL);

    List<String> getCommitArray(String projectKey, String repositoryURL, String issueId);
}
