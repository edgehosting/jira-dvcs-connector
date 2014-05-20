package com.atlassian.jira.plugins.dvcs.spi.bitbucket.activeobjects;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

import java.util.Date;

@Table("BITBUCKET_PR_CONTEXT")
public interface BitbucketPullRequestContextMapping extends Entity
{
    String REMOTE_PULL_REQUEST_ID = "REMOTE_PULL_REQUEST_ID";
    String REPOSITORY_ID = "REPOSITORY_ID";
    String LOCAL_PULL_REQUEST_ID = "LOCAL_PULL_REQUEST_ID";
    String SAVED_UPDATE_ACTIVITY = "SAVED_UPDATE_ACTIVITY";
    String COMMITS_URL = "COMMITS_URL";
    String NEXT_COMMIT = "NEXT_COMMIT";
    
    // Last update activity
    String LAST_UPDATE_ACTIVITY_STATUS = "LAST_ACTIVITY_STATUS";
    String LAST_UPDATE_ACTIVITY_DATE = "LAST_ACTIVITY_DATE";
    String LAST_UPDATE_ACTIVITY_AUTHOR = "LAST_ACTIVITY_AUTHOR";
    String LAST_UPDATE_ACTIVITY_RAW_AUTHOR = "LAST_ACTIVITY_RAW_AUTHOR";
    
    long getRemotePullRequestId();
    int getRepositoryId();
    int getLocalPullRequestId();
    boolean isSavedUpdateActivity();
    String getCommitsUrl();
    String getNextCommit();
    
    String getLastActivityStatus();
    Date getLastActivityDate();
    String getLastActivityAuthor();
    String getLastActivityRawAuthor();

    //
    // setters
    //
    void setRemotePullRequestId(long pullRequestId);
    void setRepositoryId(int repositoryId);
    void setLocalPullRequestId(int localPullRequestId);
    void setSavedUpdateActivity(boolean isSavedUpdateActivity);
    void setCommitsUrl(String commitsUrl);
    void setNextCommit(String node);
    
    void setLastActivityStatus(String status);
    void setLastActivityDate(Date date);
    void setLastActivityAuthor(String username);
    void setLastActivityRawAuthor(String rawAuthor);
}
