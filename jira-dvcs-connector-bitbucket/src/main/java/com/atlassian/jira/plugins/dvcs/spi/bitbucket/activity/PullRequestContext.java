package com.atlassian.jira.plugins.dvcs.spi.bitbucket.activity;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestUpdateActivity;

public class PullRequestContext
{
    private final Long remotePullRequestId;
    private Integer localPullRequestId;
    private BitbucketPullRequestUpdateActivity lastUpdateActivity;
    private boolean existingUpdateActivity;
    private String nextNode;
    private String commitsUrl;
    private final int repositoryId;
    
    public PullRequestContext(int repositoryId, Long remotePullRequestId)
    {
        this.repositoryId = repositoryId;
        this.remotePullRequestId = remotePullRequestId;
    }

    public String getNextNode()
    {
        return nextNode;
    }

    public void setNextNode(String nextNode)
    {
        this.nextNode = nextNode;
    }

    public Long getRemotePullRequestId()
    {
        return remotePullRequestId;
    }

    public Integer getLocalPullRequestId()
    {
        return localPullRequestId;
    }

    public void setLocalPullRequestId(Integer localPullRequestId)
    {
        this.localPullRequestId = localPullRequestId;
    }

    public BitbucketPullRequestUpdateActivity getLastUpdateActivity()
    {
        return lastUpdateActivity;
    }

    public void setLastUpdateActivity(BitbucketPullRequestUpdateActivity lastUpdateActivity)
    {
        this.lastUpdateActivity = lastUpdateActivity;
    }

    public boolean isExistingUpdateActivity() 
    {
        return existingUpdateActivity;
    }

    public void setExistingUpdateActivity(boolean existingUpdateActivity)
    {
        this.existingUpdateActivity = existingUpdateActivity;
    }

    public String getCommitsUrl()
    {
        return commitsUrl;
    }

    public void setCommitsUrl(String commitsUrl)
    {
        this.commitsUrl = commitsUrl;
    }

    public int getRepositoryId()
    {
        return repositoryId;
    }
}