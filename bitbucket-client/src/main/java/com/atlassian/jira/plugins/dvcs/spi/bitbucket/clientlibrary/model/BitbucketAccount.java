package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;

/**
 * BitbucketAccount
 *
 * <pre>
 *      {
 *           "username": "baratrion",
 *           "first_name": "Mehmet S",
 *           "last_name": "Catalbas",
 *           "avatar": "https://secure.gravatar.com/avatar/55a1369161d3a648729b59cabf160e70?d=identicon&s=32",
 *           "resource_uri": "/1.0/users/baratrion"
 *       }
 * </pre>
 * 
 * <br /><br />
 * Created on 12.7.2012, 16:43:13
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class BitbucketAccount implements Serializable
{

	private static final long serialVersionUID = 8025642439790445876L;
    
	private String username;
	
	private String first_name; 
	
	private String last_name;
	
	private String avatar;
	
	private String resource_uri;

	public BitbucketAccount()
	{
		super();
	}


	public String getUsername()
	{
		return username;
	}


	public void setUsername(String username)
	{
		this.username = username;
	}


	public String getFirst_name()
	{
		return first_name;
	}


	public void setFirst_name(String first_name)
	{
		this.first_name = first_name;
	}


	public String getLast_name()
	{
		return last_name;
	}


	public void setLast_name(String last_name)
	{
		this.last_name = last_name;
	}


	public String getAvatar()
	{
		return avatar;
	}


	public void setAvatar(String avatar)
	{
		this.avatar = avatar;
	}


	public String getResource_uri()
	{
		return resource_uri;
	}


	public void setResource_uri(String resource_uri)
	{
		this.resource_uri = resource_uri;
	}
}

