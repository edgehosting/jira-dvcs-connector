package com.atlassian.jira.plugins.dvcs.activeobjects;


import java.util.List;

/**
 * Access to the settings for this plugins state
 */
interface BitbucketProjectSettings
{
    void startSyncProgress(String projectKey, String repositoryUrl);
    void setSyncProgress(String projectKey, String repositoryUrl, int revision);
    void completeSyncProgress(String projectKey, String repositoryUrl);

    int getCount(String projectKey, String repositoryUrl, String type);
    void resetCount(String projectKey, String repositoryUrl, String type, int count);
    void incrementCommitCount(String projectKey, String repositoryUrl, String type);

    String getUsername(String projectKey, String repositoryUrl);
    void setUsername(String projectKey, String repositoryUrl, String username);
    String getPassword(String projectKey, String repositoryUrl);
    void setPassword(String projectKey, String repositoryUrl, String password);

    List<String> getCommits(String projectKey, String repositoryUrl, String issueId);
    void setCommits(String projectKey, String repositoryUrl, String issueId, List<String> commits);

    List<String> getIssueIds(String projectKey, String repositoryUrl);
    void setIssueIds(String projectKey, String repositoryUrl, List<String> issueIds);

    List<String> getRepositories(String projectKey);
    void setRepositories(String projectKey, List<String> repositories);

}
