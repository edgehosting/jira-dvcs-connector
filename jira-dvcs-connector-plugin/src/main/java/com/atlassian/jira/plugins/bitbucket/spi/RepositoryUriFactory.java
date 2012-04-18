package com.atlassian.jira.plugins.bitbucket.spi;

import com.atlassian.jira.plugins.bitbucket.api.RepositoryUri;

public interface RepositoryUriFactory
{
    RepositoryUri getRepositoryUri(String urlString);

}
