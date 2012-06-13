package com.atlassian.jira.plugins.dvcs.model;

/**
 * Used for handle DVCS groups.
 *
 */
public class Group
{

	private String slug;
	
	private String niceName;
	
	public Group()
	{
		super();
	}

	public Group(String slug)
	{
		this.slug = slug;
	}
	
	public Group(String slug, String niceName)
	{
		super();
		this.slug = slug;
		this.niceName = niceName;
	}

	public String getSlug()
	{
		return slug;
	}

	public void setSlug(String slug)
	{
		this.slug = slug;
	}

	public String getNiceName()
	{
		return niceName;
	}

	public void setNiceName(String niceName)
	{
		this.niceName = niceName;
	}
	
}
