package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;

/**
 * 
 * @author mstencel@atlassian.com
 * 
 */
public class BitbucketPullRequestLinks implements Serializable
{


    private BitbucketPullRequestLink self;
    private BitbucketPullRequestLink html;
    private BitbucketPullRequestLink decline;
    private BitbucketPullRequestLink commits;
    private BitbucketPullRequestLink comments;
    private BitbucketPullRequestLink accept;
    private BitbucketPullRequestLink activity;
    private BitbucketPullRequestLink diff;
    private BitbucketPullRequestLink approvals;

    public BitbucketPullRequestLink getSelf()
    {
        return self;
    }

    public void setSelf(final BitbucketPullRequestLink self)
    {
        this.self = self;
    }

    public BitbucketPullRequestLink getHtml()
    {
        return html;
    }

    public void setHtml(final BitbucketPullRequestLink html)
    {
        this.html = html;
    }

    public BitbucketPullRequestLink getDecline()
    {
        return decline;
    }

    public void setDecline(final BitbucketPullRequestLink decline)
    {
        this.decline = decline;
    }

    public BitbucketPullRequestLink getCommits()
    {
        return commits;
    }

    public void setCommits(final BitbucketPullRequestLink commits)
    {
        this.commits = commits;
    }

    public BitbucketPullRequestLink getComments()
    {
        return comments;
    }

    public void setComments(final BitbucketPullRequestLink comments)
    {
        this.comments = comments;
    }

    public BitbucketPullRequestLink getAccept()
    {
        return accept;
    }

    public void setAccept(final BitbucketPullRequestLink accept)
    {
        this.accept = accept;
    }

    public BitbucketPullRequestLink getActivity()
    {
        return activity;
    }

    public void setActivity(final BitbucketPullRequestLink activity)
    {
        this.activity = activity;
    }

    public BitbucketPullRequestLink getDiff()
    {
        return diff;
    }

    public void setDiff(final BitbucketPullRequestLink diff)
    {
        this.diff = diff;
    }

    public BitbucketPullRequestLink getApprovals()
    {
        return approvals;
    }

    public void setApprovals(final BitbucketPullRequestLink approvals)
    {
        this.approvals = approvals;
    }
}
