package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;

/**
 * 
 * BitbucketRepository
 * <pre>
 * {
 *     "slug": "django-piston",
 *     "name": "django-piston",
 *     "resource_uri": "/1.0/repositories/jespern/django-piston/",
 *     "followers_count": 173,
 *     "website": "",
 *     "description": "Piston is a Django mini-framework creating APIs."
 *   }
 * </pre>
 * <br /><br />
 * Created on 12.7.2012, 16:51:23
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class BitbucketRepository implements Serializable
{
	private static final long serialVersionUID = -8326622495743697198L;
	
	private String slug;
	private String name;
	private String resourceUri;
	private Integer followersCount;
	private String website;
	private String description;

	public String getSlug()
	{
		return slug;
	}

	public void setSlug(String slug)
	{
		this.slug = slug;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getResourceUri()
	{
		return resourceUri;
	}

	public void setResourceUri(String resourceUri)
	{
		this.resourceUri = resourceUri;
	}

	public Integer getFollowersCount()
	{
		return followersCount;
	}

	public void setFollowersCount(Integer followersCount)
	{
		this.followersCount = followersCount;
	}

	public String getWebsite()
	{
		return website;
	}

	public void setWebsite(String website)
	{
		this.website = website;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}
}
