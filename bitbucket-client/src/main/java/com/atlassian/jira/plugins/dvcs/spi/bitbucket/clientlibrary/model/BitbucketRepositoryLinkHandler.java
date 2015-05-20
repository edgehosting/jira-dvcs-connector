package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;

/**
 * 
 * BitbucketRepositoryLink
 *
 * <pre>
 *  {
 *        "handler": {
 *           "url": "https://jira.atlassian.com/",
 *           "display_from": "JIRA (BB)",
 *           "name": "jira",
 *           "key": "BB",
 *           "display_to": "https://jira.atlassian.com/"
 *       },...
 *   }
 * </pre>
 */
public class BitbucketRepositoryLinkHandler implements Serializable
{
	private static final long serialVersionUID = -2345157639006046210L;
	
	private String url;
	private String displayFrom;
	private String name;
	private String key;
	private String displayTo;
	
	// for custom type can be also
	private String replacementUrl;
	private String rawRegex;
	
	public String getUrl()
	{
		return url;
	}

	public void setUrl(String url)
	{
		this.url = url;
	}

	public String getDisplayFrom()
	{
		return displayFrom;
	}

	public void setDisplayFrom(String displayFrom)
	{
		this.displayFrom = displayFrom;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getKey()
	{
		return key;
	}

	public void setKey(String key)
	{
		this.key = key;
	}

	public String getDisplayTo()
	{
		return displayTo;
	}

	public void setDisplayTo(String displayTo)
	{
		this.displayTo = displayTo;
	}

    public String getReplacementUrl()
    {
        return replacementUrl;
    }

    public void setReplacementUrl(String replacementUrl)
    {
        this.replacementUrl = replacementUrl;
    }

    public String getRawRegex()
    {
        return rawRegex;
    }

    public void setRawRegex(String rawRegex)
    {
        this.rawRegex = rawRegex;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this).append(url).append(displayFrom).append(name).append(key).append(displayTo)
                .append(replacementUrl).append(rawRegex).toString();
    }
}
