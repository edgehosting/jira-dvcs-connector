package com.atlassian.jira.plugins.bitbucket.spi.bitbucket;

/**
 * Describes a repository on bitbucket
 */
public interface BitbucketRepository
{
    public String getWebsite();

    public String getName();

    public int getFollowers();

    public String getOwner();

    public String getLogo();

    public String getResourceUri();

    public String getRepositoryUrl();

    public String getSlug();

    public String getDescription();
}
