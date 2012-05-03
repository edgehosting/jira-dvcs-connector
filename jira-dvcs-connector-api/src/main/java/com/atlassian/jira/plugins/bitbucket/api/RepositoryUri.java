package com.atlassian.jira.plugins.bitbucket.api;

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

    String getFileCommitUrl(String node, String file, int counter);

    String getParentUrl(String parentNode);
}
