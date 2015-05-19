package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

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
 *       },
 *       "id": 1
 *   }
 * </pre>
 */
public class BitbucketRepositoryLink implements Serializable
{
	private static final long serialVersionUID = -2345157639006046210L;

	private Integer id;
	private BitbucketRepositoryLinkHandler handler;

	public Integer getId()
	{
		return id;
	}

	public void setId(Integer id)
	{
		this.id = id;
	}

	public BitbucketRepositoryLinkHandler getHandler()
	{
		return handler;
	}

	public void setHandler(BitbucketRepositoryLinkHandler handler)
	{
		this.handler = handler;
	}
	
	@Override
    public String toString() 
	{
        return "BitbucketRepositoryLink[" + id + ", " + handler + "]";
	}
}
