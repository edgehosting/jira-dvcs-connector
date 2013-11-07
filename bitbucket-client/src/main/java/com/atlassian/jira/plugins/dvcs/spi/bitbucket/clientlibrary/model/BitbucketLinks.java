package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;

/**
 * 
 * @author mstencel@atlassian.com
 * 
 */
public class BitbucketLinks implements Serializable
{
    private BitbucketLink self;
    private BitbucketLink html;
    private BitbucketLink decline;
    private BitbucketLink commits;
    private BitbucketLink comments;
    private BitbucketLink accept;
    private BitbucketLink activity;
    private BitbucketLink diff;
    private BitbucketLink approvals;
    private BitbucketLink reviewers;
    private BitbucketLink avatar;

    public BitbucketLink getSelf()
    {
        return self;
    }

    public void setSelf(final BitbucketLink self)
    {
        this.self = self;
    }

    public BitbucketLink getHtml()
    {
        return html;
    }

    public void setHtml(final BitbucketLink html)
    {
        this.html = html;
    }

    public BitbucketLink getDecline()
    {
        return decline;
    }

    public void setDecline(final BitbucketLink decline)
    {
        this.decline = decline;
    }

    public BitbucketLink getCommits()
    {
        return commits;
    }

    public void setCommits(final BitbucketLink commits)
    {
        this.commits = commits;
    }

    public BitbucketLink getComments()
    {
        return comments;
    }

    public void setComments(final BitbucketLink comments)
    {
        this.comments = comments;
    }

    public BitbucketLink getAccept()
    {
        return accept;
    }

    public void setAccept(final BitbucketLink accept)
    {
        this.accept = accept;
    }

    public BitbucketLink getActivity()
    {
        return activity;
    }

    public void setActivity(final BitbucketLink activity)
    {
        this.activity = activity;
    }

    public BitbucketLink getDiff()
    {
        return diff;
    }

    public void setDiff(final BitbucketLink diff)
    {
        this.diff = diff;
    }

    public BitbucketLink getApprovals()
    {
        return approvals;
    }

    public void setApprovals(final BitbucketLink approvals)
    {
        this.approvals = approvals;
    }

    public BitbucketLink getReviewers()
    {
        return reviewers;
    }

    public void setReviewers(final BitbucketLink reviewers)
    {
        this.reviewers = reviewers;
    }

    public BitbucketLink getAvatar()
    {
        return avatar;
    }

    public void setAvatar(final BitbucketLink avatar)
    {
        this.avatar = avatar;
    }
}
