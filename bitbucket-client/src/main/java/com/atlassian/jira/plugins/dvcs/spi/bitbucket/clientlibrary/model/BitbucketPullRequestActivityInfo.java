package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

public class BitbucketPullRequestActivityInfo
{
    private BitbucketPullRequestBaseActivity activity;
    private BitbucketPullRequest pullRequest;

    public BitbucketPullRequestActivityInfo()
    {
        super();
    }

    public BitbucketPullRequestBaseActivity getActivity()
    {
        return activity;
    }

    public void setActivity(BitbucketPullRequestBaseActivity activity)
    {
        this.activity = activity;
    }

    public BitbucketPullRequest getPullRequest()
    {
        return pullRequest;
    }

    public void setPullRequest(BitbucketPullRequest pullRequest)
    {
        this.pullRequest = pullRequest;
    }

}