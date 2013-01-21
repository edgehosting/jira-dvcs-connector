package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

public class BitbucketPullRequestActivityInfo
{
    private BitbucketPullRequestBaseActivity activity;
    private BitbucketPullRequest pr;

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

    public BitbucketPullRequest getPr()
    {
        return pr;
    }

    public void setPr(BitbucketPullRequest pr)
    {
        this.pr = pr;
    }
}