package com.atlassian.jira.plugins.bitbucket.spi.github;

import com.atlassian.jira.plugins.bitbucket.api.impl.DefaultRepositoryUri;

import java.text.MessageFormat;

public class GithubRepositoryUri extends DefaultRepositoryUri
{
    public GithubRepositoryUri(String protocol, String hostname, String owner, String slug)
    {
        super(protocol, hostname, owner, slug);
    }

    @Override
    public String getApiUrl()
    {
//        return MessageFormat.format("{0}://{1}/api/v2/json", getProtocol(), getHostname());
        return "https://api.github.com";
    }

    @Override
    public String getCommitUrl(String node)
    {
        return MessageFormat.format("{0}://{1}/{2}/{3}/commit/{4}", getProtocol(), getHostname(), getOwner(), getSlug(), node);
    }

    @Override
    public String getUserUrl(String username)
    {
        return MessageFormat.format("{0}://{1}/{2}", getProtocol(), getHostname(), username);
    }

    @Override
    public String getRepositoryInfoUrl()
    {
        return MessageFormat.format("/repos/{0}/{1}", getOwner(), getSlug());
    }

    @Override
    public String getFileCommitUrl(String node, String file, int counter)
    {
        return MessageFormat.format("{0}#diff-{1}", getCommitUrl(node), counter);
    }

    @Override
    public String getParentUrl(String parentNode)
    {
        return MessageFormat.format("{0}://{1}/{2}/{3}/commit/{4}", getProtocol(), getHostname(), getOwner(), getSlug(), parentNode);
    }
}