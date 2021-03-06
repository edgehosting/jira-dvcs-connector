package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;

/**
 *
 * @author mstencel@atlassian.com
 */
public class BitbucketPullRequestHead implements Serializable
{
    private static final long serialVersionUID = -5134849227580638384L;

    private BitbucketPullRequestCommit commit;
    private BitbucketBranch branch;
    private BitbucketPullRequestRepository repository;

    public BitbucketPullRequestCommit getCommit()
    {
        return commit;
    }

    public void setCommit(BitbucketPullRequestCommit commit)
    {
        this.commit = commit;
    }

    public BitbucketPullRequestRepository getRepository()
    {
        return repository;
    }

    public void setRepository(BitbucketPullRequestRepository repository)
    {
        this.repository = repository;
    }

    public BitbucketBranch getBranch()
    {
        return branch;
    }

    public void setBranch(BitbucketBranch branchName)
    {
        this.branch = branchName;
    }
}