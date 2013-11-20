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

    private BitbucketLinks links;

    private String fullName;

    public BitbucketPullRequestRepository()
    {
        super();
    }

    public BitbucketPullRequestRepository(final String owner, final String slug)
    {
        this.owner = owner;
        this.slug = slug;
    }

    public BitbucketPullRequestRepository(final String fullName)
    {
        this.fullName = fullName;
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

    public BitbucketLinks getLinks()
    {
        return links;
    }

    public void setLinks(BitbucketLinks links)
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

