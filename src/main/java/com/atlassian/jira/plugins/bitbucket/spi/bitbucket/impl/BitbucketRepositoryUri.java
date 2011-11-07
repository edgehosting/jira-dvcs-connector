package com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl;

import com.atlassian.jira.plugins.bitbucket.spi.DefaultRepositoryUri;

import java.text.MessageFormat;

/**
 * Used to identify a repository, contains an owner, and a slug 
 */
public class BitbucketRepositoryUri extends DefaultRepositoryUri
{
    public BitbucketRepositoryUri(String protocol, String hostname, String owner, String slug)
    {
        super(protocol, hostname, owner, slug);
    }

    public String getApiUrl()
    {
    	return MessageFormat.format("{0}://api.{1}/1.0", getProtocol(), getHostname());
    }
    
    public String getCommitUrl(String node)
    {
    	return MessageFormat.format("{0}://{1}/{2}/{3}/changeset/{4}", getProtocol(), getHostname(), getOwner(), getSlug(), node);
    }

    public String getUserUrl(String username)
    {
    	return MessageFormat.format("{0}://{1}/{2}", getProtocol(), getHostname(), username);
    }
}
