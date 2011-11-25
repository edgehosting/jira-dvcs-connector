package com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl;

import com.atlassian.jira.plugins.bitbucket.spi.CustomStringUtils;
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

    @Override
    public String getApiUrl()
    {
    	return MessageFormat.format("{0}://api.{1}/1.0", getProtocol(), getHostname());
    }
    
    @Override
    public String getCommitUrl(String node)
    {
    	return MessageFormat.format("{0}://{1}/{2}/{3}/changeset/{4}", getProtocol(), getHostname(), getOwner(), getSlug(), node);
    }

    @Override
    public String getUserUrl(String username)
    {
    	return MessageFormat.format("{0}://{1}/{2}", getProtocol(), getHostname(), username);
    }

    @Override
    public String getRepositoryInfoUrl()
    {
        return MessageFormat.format("/repositories/{0}/{1}", CustomStringUtils.encode(getOwner()), CustomStringUtils.encode(getSlug()));  
    }

    @Override
    public String getFileCommitUrl(String node, String file)
    {
        return MessageFormat.format("{0}://{1}/{2}/{3}/src/{4}/{5}", getProtocol(), getHostname(), getOwner(), getSlug(), node, file);
    }
}
