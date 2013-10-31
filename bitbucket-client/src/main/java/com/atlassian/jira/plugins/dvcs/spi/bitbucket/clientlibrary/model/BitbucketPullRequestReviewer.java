package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

/**
 * Bitbucket PullRequest Reviewer
 *
 * @author mstencel@atlassian.com
 */
public class BitbucketPullRequestReviewer
{
    private BitbucketUser user;
    private BitbucketLinks links;

    public BitbucketUser getUser()
    {
        return user;
    }

    public void setUser(final BitbucketUser user)
    {
        this.user = user;
    }

    public BitbucketLinks getLinks()
    {
        return links;
    }

    public void setLinks(final BitbucketLinks links)
    {
        this.links = links;
    }
}
