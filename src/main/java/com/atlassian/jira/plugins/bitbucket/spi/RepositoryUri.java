package com.atlassian.jira.plugins.bitbucket.spi;

//TODO Javadoc
public interface RepositoryUri
{
    String getOwner();

    String getSlug();

    String getBaseUrl();

    String getRepositoryUrl();

    String getApiUrl();

    String getCommitUrl(String node);

    String getUserUrl(String username);

    String getRepositoryInfoUrl();
}
