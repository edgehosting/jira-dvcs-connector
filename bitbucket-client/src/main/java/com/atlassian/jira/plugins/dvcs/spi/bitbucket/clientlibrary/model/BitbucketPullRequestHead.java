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
    private BitbucketBranch branchName;
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

    public BitbucketBranch getBranchName()
    {
        return branchName;
    }

    public void setBranchName(BitbucketBranch branchName)
    {
        this.branchName = branchName;
    }
}