package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;

/**
 * BitbucketAccount represents both Bitbucket user and team.
 *
 * <pre>
 *      {
 *           "username": "baratrion",
 *           "first_name": "Mehmet S",
 *           "last_name": "Catalbas",
 *           "avatar": "https://secure.gravatar.com/avatar/55a1369161d3a648729b59cabf160e70?d=identicon&amp;s=32",
 *           "resource_uri": "/1.0/users/baratrion"
 *       }
 * </pre>
 *
 * @author jhocman@atlassian.com
 *
 */
public class BitbucketAccount implements Serializable
{
	private String username;
	private String firstName; 
	private String lastName;
	private String avatar;
    private String resourceUri;

    public String getUsername()
	{
		return username;
	}

	public void setUsername(String username)
	{
		this.username = username;
	}

	public String getFirstName()
	{
		return firstName;
	}

	public void setFirstName(String firstName)
	{
		this.firstName = firstName;
	}

	public String getLastName()
	{
		return lastName;
	}

	public void setLastName(String lastName)
	{
		this.lastName = lastName;
	}

	public String getAvatar()
	{
		return avatar;
	}

	public void setAvatar(String avatar)
	{
		this.avatar = avatar;
	}

    public String getResourceUri()
    {
        return resourceUri;
    }

    public void setResource_uri(String resourceUri)
    {
        this.resourceUri = resourceUri;
    }
}
