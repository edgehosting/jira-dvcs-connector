package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.util.ArrayList;
import java.util.List;

public class BitbucketPullRequestActivityInfo implements HasMessages
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

    @Override
    public List<String> getMessages()
    {
        List<String> ret = new ArrayList<String>();
        ret.addAll(pullRequest.getMessages());
        ret.addAll(activity.getMessages());
        return ret;
    }

}