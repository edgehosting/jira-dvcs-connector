package com.atlassian.jira.plugins.bitbucket.spi;

public interface RepositoryUriFactory
{
    RepositoryUri getRepositoryUri(String urlString);

}
