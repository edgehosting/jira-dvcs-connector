package com.atlassian.jira.plugins.bitbucket.spi.bitbucket;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;

/**
 * Used to identify a repository, contains an owner, and a slug 
 */
public class RepositoryUri
{
    public static RepositoryUri parse(String urlString)
    {
    	try
        {
    		URL url = new URL(urlString);
    		String protocol = url.getProtocol();
    		String hostname = url.getHost();
    		String path = url.getPath();
    		String[] split = StringUtils.split(path,"/");
    		if (split.length<2)
    		{
    			throw new SourceControlException("Expected url is https://domainname.com/username/repository");
    		}
    		String owner = split[0];
    		String slug = split[1];
    		return new RepositoryUri(protocol, hostname, owner, slug);
        }
        catch (MalformedURLException e)
        {
        	throw new SourceControlException("Invalid url ["+urlString+"]");
        }
    }

    private final String protocol;
    private final String owner;
    private final String slug;
	private final String hostname;

    private RepositoryUri(String protocol, String hostname, String owner, String slug)
    {
        this.protocol = protocol;
		this.hostname = hostname;
		this.owner = owner;
        this.slug = slug;
    }

    public String getOwner()
    {
        return owner;
    }

    public String getSlug()
    {
        return slug;
    }
    
    public String getBaseUrl()
    {
    	return MessageFormat.format("{0}://{1}", protocol, hostname);
    }
    
    public String getRepositoryUrl()
    {
    	return MessageFormat.format("{0}://{1}/{2}/{3}", protocol, hostname, owner, slug);
    }

    public String getApiUrl()
    {
    	return MessageFormat.format("{0}://api.{1}/1.0", protocol, hostname, owner, slug);
    }
    
    public String getCommitUrl(String node)
    {
    	return MessageFormat.format("{0}://{1}/{2}/{3}/changeset/{4}", protocol, hostname, owner, slug, node);
    }

    public String getUserUrl(String username)
    {
    	return MessageFormat.format("{0}://{1}/{2}", protocol, hostname, username);
    }
}
