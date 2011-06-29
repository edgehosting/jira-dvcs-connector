package com.atlassian.jira.plugins.bitbucket.bitbucket;

/**
 * Details about a file that has changed in a {@link BitbucketChangeset}.
 */
public interface BitbucketChangesetFile
{
    public BitbucketChangesetFileType getType();

    public String getFile();
}
