package com.atlassian.jira.plugins.bitbucket.common.bitbucket;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.atlassian.jira.plugins.bitbucket.common.SourceControlRepository;

//TODO rename to DefaultSourceControlRepository
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
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj == null) return false;
		if (this==obj) return true;
		if (this.getClass()!=obj.getClass()) return false;
		BitbucketRepository that = (BitbucketRepository) obj;
		new EqualsBuilder().append(id, that.id).append(url, that.url)
			.append(projectKey, that.projectKey).append(username, that.username)
			.append(password, that.password);
		return super.equals(obj);
	}
	
	@Override
	public int hashCode()
	{
		return new HashCodeBuilder(17,37)
			.append(id).append(url).append(projectKey)
			.append(username).append(password).toHashCode();
	}
}
