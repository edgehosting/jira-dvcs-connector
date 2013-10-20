package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;


/**
 *
 *
 * ...
 *        "pr_repo": {
 *            "owner": "jhocman",
 *            "slug": "hgrepoindahouse"
 *        },
 *     ...
 *
 *
 */
public class BitbucketPullRequestRepository
{

    private String owner;

    private String slug;

    private BitbucketPullRequestLinks links;

    private String fullName;

    public BitbucketPullRequestRepository()
    {
        super();
    }

    public String getOwner()
    {
        return owner;
    }

    public void setOwner(String owner)
    {
        this.owner = owner;
    }

    public String getSlug()
    {
        return slug;
    }

    public void setSlug(String slug)
    {
        this.slug = slug;
    }

    public BitbucketPullRequestLinks getLinks()
    {
        return links;
    }

    public void setLinks(BitbucketPullRequestLinks links)
    {
        this.links = links;
    }

    public String getFullName()
    {
        return fullName;
    }

    public void setFullName(String fullName)
    {
        this.fullName = fullName;
    }
}

