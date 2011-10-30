package com.atlassian.jira.plugins.bitbucket.rest;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "repository")
@XmlAccessorType(XmlAccessType.FIELD)
public class Repository
{
    @XmlAttribute
    private int id;

    @XmlAttribute
    private String projectKey;
    
    @XmlAttribute
    private String url;

    @XmlElement
    private SyncProgress sync;

    @XmlAttribute
    private String username;

    @XmlAttribute
    private String password;
    
    @XmlAttribute
    private String adminUsername;

	@XmlAttribute
    private String adminPassword;

	public Repository()
    {
    }

    public Repository(int id, String projectKey, String url, String username, String password, String adminUsername, String adminPassword)
    {
        this.id = id;
        this.projectKey = projectKey;
        this.url = url;
		this.username = username;
		this.password = password;
		this.adminUsername = adminUsername;
		this.adminPassword = adminPassword;
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
    public String getUsername()
	{
		return username;
	}

	public void setUsername(String username)
	{
		this.username = username;
	}

	public String getPassword()
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
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
}
