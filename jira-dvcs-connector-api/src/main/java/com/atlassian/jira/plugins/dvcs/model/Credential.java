package com.atlassian.jira.plugins.dvcs.model;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Credential
{
    @Deprecated
    private String adminUsername;
    @Deprecated
    private String adminPassword;


    private String accessToken;

    // case of 2LO oauth
    private String oauthKey;
    private String oauthSecret;

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


    public Credential(String adminUsername, String adminPassword, String accessToken, String oauthKey,
            String oauthSecret)
    {
        this(adminUsername, adminPassword, accessToken);
        this.oauthKey = oauthKey;
        this.oauthSecret = oauthSecret;
    }

    @Deprecated
    public String getAdminUsername()
    {
        return adminUsername;
    }

    @Deprecated
    public String getAdminPassword()
    {
        return adminPassword;
    }

    @Deprecated
    public void setAdminPassword(String adminPassword)
    {
        this.adminPassword = adminPassword;
    }

    public String getAccessToken()
    {
        return accessToken;
    }

    @Deprecated
	public void setAdminUsername(String adminUsername)
	{
		this.adminUsername = adminUsername;
	}

	public void setAccessToken(String accessToken)
	{
		this.accessToken = accessToken;
	}

    public String getOauthKey()
    {
        return oauthKey;
    }

    public void setOauthKey(String oauthKey)
    {
        this.oauthKey = oauthKey;
    }

    public String getOauthSecret()
    {
        return oauthSecret;
    }

    public void setOauthSecret(String oauthSecret)
    {
        this.oauthSecret = oauthSecret;
    }
}
