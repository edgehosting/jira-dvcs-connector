package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;

/**
 * BitbucketServiceField
 *
 * <pre>
 *           {
 *               "name": "URL",
 *               "value": "https://bitbucket.org/post"
 *           }
 * </pre>
 * 
 * <br /><br />
 * Created on 12.7.2012, 17:00:19
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class BitbucketServiceField implements Serializable
{
	private static final long serialVersionUID = -1942066377818593354L;
	
	private String name;
	private String value;

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}
}
