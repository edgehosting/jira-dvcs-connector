package com.atlassian.jira.plugins.bitbucket.rest;

import javax.xml.bind.annotation.*;

@XmlRootElement(name = "repository")
@XmlAccessorType(XmlAccessType.FIELD)
public class Repository
{
    @XmlAttribute
    private int id;

    @XmlAttribute
    private String repositoryType;
    
    @XmlAttribute
    private String projectKey;
    
    @XmlAttribute
    private String url;

    @XmlElement
    private SyncProgress sync;

    @XmlAttribute
    private String adminUsername;
    
    @XmlAttribute
    private String adminPassword;

	@XmlAttribute
    private String accessToken;

    @XmlAttribute
    private String lastCommitRelativeDate;

	public Repository()
    {
    }

    public Repository(int id, String repositoryType, String projectKey, String url, String adminUsername, String adminPassword, String accessToken, String lastCommitRelativeDate)
    {
        this.id = id;
        this.repositoryType = repositoryType;
        this.projectKey = projectKey;
        this.url = url;
		this.adminUsername = adminUsername;
		this.adminPassword = adminPassword;
        this.accessToken = accessToken;
        this.lastCommitRelativeDate = lastCommitRelativeDate;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getProjectKey()
    {
        return projectKey;
    }

    public void setProjectKey(String projectKey)
    {
        this.projectKey = projectKey;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public SyncProgress getSync()
    {
        return sync;
    }

    public void setStatus(SyncProgress sync)
    {
        this.sync = sync;
    }

    public String getAdminUsername()
	{
		return adminUsername;
	}

	public void setAdminUsername(String adminUsername)
	{
		this.adminUsername = adminUsername;
	}

	public String getAdminPassword()
	{
		return adminPassword;
	}

	public void setAdminPassword(String adminPassword)
	{
		this.adminPassword = adminPassword;
	}

    public String getRepositoryType()
    {
        return repositoryType;
    }

    public void setRepositoryType(String repositoryType)
    {
        this.repositoryType = repositoryType;
    }

    public String getAccessToken()
    {
        return accessToken;
    }

    public void setAccessToken(String accessToken)
    {
        this.accessToken = accessToken;
    }

    public String getLastCommitRelativeDate() {
        return lastCommitRelativeDate;
    }

    public void setLastCommitRelativeDate(String lastCommitRelativeDate) {
        this.lastCommitRelativeDate = lastCommitRelativeDate;
    }
}
