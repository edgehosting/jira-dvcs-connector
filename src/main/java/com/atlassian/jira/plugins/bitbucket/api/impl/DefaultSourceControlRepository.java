package com.atlassian.jira.plugins.bitbucket.api.impl;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;

public class DefaultSourceControlRepository implements SourceControlRepository
{
	private final int id;
	private final String url;
	private final String username;
	private final String password;
	private final String projectKey;
	private final String adminUsername;
	private final String adminPassword;

	public DefaultSourceControlRepository(int id, String url, String projectKey, String username, String password,
			String adminUsername, String adminPassword)
	{
		this.id = id;
		this.url = url;
		this.projectKey = projectKey;
		this.username = username;
		this.password = password;
		this.adminUsername = adminUsername;
		this.adminPassword = adminPassword;
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

	public String getAdminUsername()
	{
		return adminUsername;
	}

	public String getAdminPassword()
	{
		return adminPassword;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj == null) return false;
		if (this==obj) return true;
		if (this.getClass()!=obj.getClass()) return false;
		DefaultSourceControlRepository that = (DefaultSourceControlRepository) obj;
		return new EqualsBuilder().append(id, that.id).append(url, that.url)
			.append(projectKey, that.projectKey).append(username, that.username)
			.append(password, that.password).append(adminUsername, that.adminUsername)
			.append(adminPassword, that.adminPassword).isEquals();
	}
	
	@Override
	public int hashCode()
	{
		return new HashCodeBuilder(17,37)
			.append(id).append(url).append(projectKey)
			.append(username).append(password).append(adminUsername).append(adminPassword).toHashCode();
	}
}
