package com.atlassian.jira.plugins.dvcs.model;

import com.atlassian.gzipfilter.org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Credential implements Serializable
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

    public Credential(String oauthKey, String oauthSecret, String accessToken)
    {
        this.oauthKey = oauthKey;
        this.oauthSecret = oauthSecret;
        this.accessToken = accessToken;
    }

    @Deprecated
    public Credential(String oauthKey, String oauthSecret, String accessToken, String adminUsername,
            String adminPassword)
    {
        this(oauthKey, oauthSecret, accessToken);
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
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
    
    @Override
    public boolean equals(Object other)
    {
        Credential o = (Credential) other;
        return new EqualsBuilder()
            .append(adminUsername, o.adminUsername)
            .append(adminPassword, o.adminPassword)
            .append(accessToken, o.accessToken)
            .append(oauthKey, o.oauthKey)
            .append(oauthSecret, o.oauthSecret)
            .isEquals();
    }
    
    @Override
    public int hashCode()
    {
        return new HashCodeBuilder()
            .append(adminUsername)
            .append(adminPassword)
            .append(accessToken)
            .append(oauthKey)
            .append(oauthSecret)
            .hashCode();
    }
}
