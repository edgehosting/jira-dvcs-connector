package com.atlassian.jira.plugins.bitbucket.api.rest;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "accountinfo")
@XmlAccessorType(XmlAccessType.FIELD)
@Deprecated
public class AccountInfo
{
    @XmlAttribute
    private String username;
    
    @XmlAttribute
    private String avatarUrl;

    @XmlAttribute
    private String accountType;

    @XmlAttribute
    private String server;
    
    @XmlAttribute
    private boolean requiresOauth;

    public AccountInfo()    {}
    
    public AccountInfo(String server, String username, String avatarUrl, String accountType)
    {
        this.server = server;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.accountType = accountType;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getAvatarUrl()
    {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl)
    {
        this.avatarUrl = avatarUrl;
    }

    public String getAccountType()
    {
        return accountType;
    }

    public void setAccountType(String accountType)
    {
        this.accountType = accountType;
    }

    public String getServer()
    {
        return server;
    }

    public void setServer(String server)
    {
        this.server = server;
    }

	public boolean isRequiresOauth()
	{
		return requiresOauth;
	}

	public void setRequiresOauth(boolean requiresOauth)
	{
		this.requiresOauth = requiresOauth;
	}

}
