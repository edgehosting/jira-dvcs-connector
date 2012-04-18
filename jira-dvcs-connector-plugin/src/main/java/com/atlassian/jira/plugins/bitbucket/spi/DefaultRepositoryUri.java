package com.atlassian.jira.plugins.bitbucket.spi;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.atlassian.jira.plugins.bitbucket.api.RepositoryUri;

import java.text.MessageFormat;

public abstract class DefaultRepositoryUri implements RepositoryUri
{
    private final String protocol;
    private final String owner;
    private final String slug;
	private final String hostname;

    public DefaultRepositoryUri(String protocol, String hostname, String owner, String slug)
    {
        this.protocol = protocol;
		this.hostname = hostname;
		this.owner = owner;
        this.slug = slug;
    }


    public String getProtocol()
    {
        return protocol;
    }

    public String getOwner()
    {
        return owner;
    }

    public String getSlug()
    {
        return slug;
    }

    public String getHostname()
    {
        return hostname;
    }

    public String getBaseUrl()
    {
    	return MessageFormat.format("{0}://{1}", getProtocol(), getHostname());
    }

    public String getRepositoryUrl()
    {
    	return MessageFormat.format("{0}://{1}/{2}/{3}", getProtocol(), getHostname(), getOwner(), getSlug());
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DefaultRepositoryUri that = (DefaultRepositoryUri) o;

        return new EqualsBuilder().append(hostname, that.hostname).
                append(owner, that.owner).append(protocol, that.protocol).append(slug, that.slug).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17,37).append(protocol).append(owner).append(slug).append(hostname).toHashCode();
    }


}
