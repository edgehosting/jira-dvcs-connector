package com.atlassian.jira.plugins.bitbucket.rest;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
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

    @XmlAttribute
    private String status;

    @XmlAttribute
    private String username;

    @XmlAttribute
    private String password;

	public Repository()
    {
    }

    public Repository(int id, String projectKey, String url, String username, String password)
    {
        this.id = id;
        this.projectKey = projectKey;
        this.url = url;
		this.username = username;
		this.password = password;
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

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
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
}
