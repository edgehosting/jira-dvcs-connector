package com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl;

import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketRepository;

/**
 * Describes a repository on bitbucket
 */
public class DefaultBitbucketRepository implements BitbucketRepository
{
    private final String website;
    private final String name;
    private final int followers;
    private final String owner;
    private final String logo;
    private final String resourceUri;
    private final String slug;
    private final String description;

    public DefaultBitbucketRepository(String website, String name, int followers, String owner,
                                      String logo, String resourceUri, String slug, String description)
    {
        this.website = website;
        this.name = name;
        this.followers = followers;
        this.owner = owner;
        this.logo = logo;
        this.resourceUri = resourceUri;
        this.slug = slug;
        this.description = description;
    }

    public String getWebsite()
    {
        return website;
    }

    public String getName()
    {
        return name;
    }

    public int getFollowers()
    {
        return followers;
    }

    public String getOwner()
    {
        return owner;
    }

    public String getLogo()
    {
        return logo;
    }

    public String getResourceUri()
    {
        return resourceUri;
    }

    public String getRepositoryUrl()
    {
        return "https://bitbucket.org/"+owner+"/"+slug;
    }

    public String getSlug()
    {
        return slug;
    }

    public String getDescription()
    {
        return description;
    }
}
