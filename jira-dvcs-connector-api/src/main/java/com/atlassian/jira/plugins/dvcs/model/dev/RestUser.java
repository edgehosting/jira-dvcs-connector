package com.atlassian.jira.plugins.dvcs.model.dev;

import com.google.gson.annotations.SerializedName;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType (XmlAccessType.FIELD)
public class RestUser
{
    private String avatar;
    @SerializedName("emailAddress")
    private String emailAddress;
    private String name;
    private String username;

    public RestUser()
    {
    }

    public RestUser(final String username, final String name, final String emailAddress, final String avatar)
    {
        this.username = username;
        this.name = name;
        this.emailAddress = emailAddress;
        this.avatar = avatar;
    }

    public String getName()
    {
        return name;
    }

    public void setName(final String name)
    {
        this.name = name;
    }

    public String getEmailAddress()
    {
        return emailAddress;
    }

    public void setEmailAddress(final String emailAddress)
    {
        this.emailAddress = emailAddress;
    }

       public String getAvatar()
    {
        return avatar;
    }

    public void setAvatar(final String avatar)
    {
        this.avatar = avatar;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(final String username)
    {
        this.username = username;
    }
}
