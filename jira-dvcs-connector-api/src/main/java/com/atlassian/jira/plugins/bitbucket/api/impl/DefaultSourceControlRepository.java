package com.atlassian.jira.plugins.bitbucket.api.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.atlassian.jira.plugins.bitbucket.api.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;

public class DefaultSourceControlRepository implements SourceControlRepository
{
	private final int id;
	private final String repositoryName;
    private final RepositoryUri repositoryUri;
    private final String projectKey;
	private final String adminUsername;
	private final String adminPassword;
    private final String repositoryType;
    private final String accessToken;

    public DefaultSourceControlRepository(int id, String repositoryName, String repositoryType, RepositoryUri repositoryUri, String projectKey,
                                          String adminUsername, String adminPassword, String accessToken)
	{
		this.id = id;
        this.repositoryName = repositoryName;
        this.repositoryUri = repositoryUri;
        this.projectKey = projectKey;
		this.adminUsername = adminUsername;
		this.adminPassword = adminPassword;
        this.repositoryType = repositoryType;
        this.accessToken = accessToken;
    }
    
	@Override
    public int getId()
	{
		return id;
	}

	@Override
	public String getRepositoryType()
	{
	    return repositoryType;
	}

	@Override
    public String getRepositoryName()
    {
	    if (StringUtils.isBlank(repositoryName))
	    {
	        return repositoryUri.getSlug();
	    }
        return repositoryName;
    }

	@Override
    public RepositoryUri getRepositoryUri()
    {
        return repositoryUri;
    }

    @Override
    public String getProjectKey()
	{
		return projectKey;
	}
	

	@Override
    public String getAdminUsername()
	{
		return adminUsername;
	}

	@Override
    public String getAdminPassword()
	{
		return adminPassword;
	}

	@Override
    public String getAccessToken()
	{
	    return accessToken;
	}

    @Override
	public boolean equals(Object obj)
	{
		if (obj == null) return false;
		if (this==obj) return true;
		if (this.getClass()!=obj.getClass()) return false;
		DefaultSourceControlRepository that = (DefaultSourceControlRepository) obj;
		return new EqualsBuilder().append(id, that.id).append(repositoryUri, that.repositoryUri).append(repositoryName, that.repositoryName)
			.append(projectKey, that.projectKey)
			.append(adminUsername, that.adminUsername)
			.append(adminPassword, that.adminPassword).append(accessToken, that.accessToken)
            .isEquals();
	}
	
	@Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).append(id).append(repositoryUri).append(repositoryName).append(projectKey)
            .append(adminUsername).append(adminPassword).append(accessToken)
            .toHashCode();
    }
}
