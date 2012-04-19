package com.atlassian.jira.plugins.bitbucket.api;


public interface RepositoryUriFactory
{
    RepositoryUri getRepositoryUri(String urlString);

}
