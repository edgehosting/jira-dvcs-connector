package com.atlassian.jira.plugins.bitbucket.spi.github.impl;

import java.text.MessageFormat;

import com.atlassian.jira.plugins.bitbucket.spi.DefaultRepositoryUri;

public class GithubRepositoryUri extends DefaultRepositoryUri
{
    public GithubRepositoryUri(String protocol, String hostname, String owner, String slug)
    {
        super(protocol, hostname, owner, slug);
    }

    @Override
    public String getApiUrl()
    {
        return MessageFormat.format("{0}://{1}/api/v2/json", getProtocol(), getHostname());
    }

    @Override
    public String getCommitUrl(String node)
    {
        return null;
    }

    @Override
    public String getUserUrl(String username)
    {
        return null;
    }

    @Override
    public String getRepositoryInfoUrl()
    {
        return MessageFormat.format("/repos/show/{0}/{1}", getOwner(), getSlug());
    }
}
