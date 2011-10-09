package com.atlassian.jira.plugins.bitbucket.common.bitbucket;

import com.atlassian.jira.plugins.bitbucket.common.SourceControlRepository;

public class BitbucketRepository implements SourceControlRepository
{
	private final int id;
	private final String url;
	private final String username;
	private final String password;
	private final String projectKey;

	public BitbucketRepository(int id, String url, String projectKey, String username, String password)
	{
		this.id = id;
		this.url = url;
		this.projectKey = projectKey;
		this.username = username;
		this.password = password;
	}
	public int getId()
	{
		return id;
	}
	
	public String getUrl()
	{
		return url;
	}
	
	public String getProjectKey()
	{
		return projectKey;
	}
	

	public String getUsername()
	{
		return username;
	}
	
	public String getPassword()
	{
		return password;
	}
}
