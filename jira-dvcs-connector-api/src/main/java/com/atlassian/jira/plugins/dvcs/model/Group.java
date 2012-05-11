package com.atlassian.jira.plugins.dvcs.model;

/**
 * Used for handle DVCS groups.
 *
 */
public class Group
{

	private String slug;
	
	public Group()
	{
		super();
	}

	public Group(String slug)
	{
		this.slug = slug;
	}

	public String getSlug()
	{
		return slug;
	}

	public void setSlug(String slug)
	{
		this.slug = slug;
	}
	
}
