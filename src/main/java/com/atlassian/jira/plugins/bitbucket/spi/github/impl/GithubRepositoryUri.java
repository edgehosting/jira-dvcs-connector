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
        return MessageFormat.format("{0}://{1}/{2}/{3}/commit/{4}", getProtocol(), getHostname(), getOwner(), getSlug(), node);
    }

    public String getUserUrl(String username)
    {
        return MessageFormat.format("{0}://{1}/{2}", getProtocol(), getHostname(), username);
    }
}
