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


}

