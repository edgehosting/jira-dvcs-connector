package com.atlassian.jira.plugins.bitbucket.spi.github.impl;

import com.atlassian.jira.plugins.bitbucket.spi.DefaultRepositoryUri;

import java.text.MessageFormat;

public class GithubRepositoryUri extends DefaultRepositoryUri
{
    public GithubRepositoryUri(String protocol, String hostname, String owner, String slug)
    {
        super(protocol, hostname, owner, slug);
    }

    public String getApiUrl()
    {
        return MessageFormat.format("{0}://{1}/api/v2/json", getProtocol(), getHostname());
    }

    public String getCommitUrl(String node)
    {
        return null;
    }

    public String getUserUrl(String username)
    {
        return null;
    }
}
