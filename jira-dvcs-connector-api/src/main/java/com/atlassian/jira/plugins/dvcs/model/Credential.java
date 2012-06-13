package com.atlassian.jira.plugins.dvcs.model;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Credential
{
    private String adminUsername;
    private String adminPassword;
    private String accessToken;

    public Credential()
	{
    	super();
	}
    
    public Credential(String adminUsername, String adminPassword, String accessToken)
    {
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.accessToken = accessToken;
    }

    public String getAdminUsername()
    {
        return adminUsername;
    }

    public String getAdminPassword()
    {
        return adminPassword;
    }

    public String getAccessToken()
    {
        return accessToken;
    }

	public void setAdminUsername(String adminUsername)
	{
		this.adminUsername = adminUsername;
	}

	public void setAccessToken(String accessToken)
	{
		this.accessToken = accessToken;
	}
}
