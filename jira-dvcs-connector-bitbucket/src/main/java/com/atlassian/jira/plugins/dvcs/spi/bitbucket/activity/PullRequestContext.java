package com.atlassian.jira.plugins.dvcs.spi.bitbucket.activity;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.activeobjects.BitbucketPullRequestCommitMapping;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestUpdateActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.dao.BitbucketPullRequestDao;

public class PullRequestContext implements Iterator<BitbucketPullRequestCommitMapping>, Iterable<BitbucketPullRequestCommitMapping>
{
    private final Long remotePullRequestId;
    private Integer localPullRequestId;
    private BitbucketPullRequestUpdateActivity lastUpdateActivity;
    private boolean existingUpdateActivity;
    private final BitbucketPullRequestDao pullRequestDao;
    private String nextNode;
    private String commitsUrl;
    private final int repositoryId;
    
    public PullRequestContext(int repositoryId, Long remotePullRequestId, BitbucketPullRequestDao pullRequestDao)
    {
        this.repositoryId = repositoryId;
        this.remotePullRequestId = remotePullRequestId;
        this.pullRequestDao = pullRequestDao;
    }
    
    public Iterable<BitbucketPullRequestCommitMapping> getCommitIterator()
    {
        return this;
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

    @Override
    public boolean hasNext()
    {
        return nextNode != null;
    }

    @Override
    public BitbucketPullRequestCommitMapping next()
    {
        if (!hasNext())
        {
            throw new NoSuchElementException();
        }
        BitbucketPullRequestCommitMapping mapping = pullRequestDao.getCommitForPullRequest(localPullRequestId, nextNode);
        if (mapping != null)
        {
            nextNode = mapping.getNextNode();
        } else
        {
            nextNode = null;
        }
        return mapping;
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<BitbucketPullRequestCommitMapping> iterator()
    {
        return this;
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