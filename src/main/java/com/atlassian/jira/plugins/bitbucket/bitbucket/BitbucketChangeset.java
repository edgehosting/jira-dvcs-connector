package com.atlassian.jira.plugins.bitbucket.bitbucket;

/**
 * Details on a changeset found in Bitbucket.
 */
public interface BitbucketChangeset
{
    public String getNode();

    public String getRawAuthor();

    public String getAuthor();

    public String getTimestamp();

    public String getRawNode();

    public String getBranch();

    public String getMessage();

    public int getRevision();

    public String getRepositoryOwner();

    public String getRepositorySlug();

}
