package com.atlassian.jira.plugins.dvcs.model.dev;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType (XmlAccessType.FIELD)
public class RestAuthor
{
    private String name;
    private String username;
    private String emailAddress;
    private String avatar;
    private String url;

    public RestAuthor()
    {
    }

    public RestAuthor(final String name, final String username, final String emailAddress, final String avatar, final String url)
    {
        this.name = name;
        this.username = username;
        this.emailAddress = emailAddress;
        this.avatar = avatar;
        this.url = url;
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

    public String getUsername()
    {
        return username;
    }

    public void setUsername(final String username)
    {
        this.username = username;
    }

    public String getAvatar()
    {
        return avatar;
    }

    public void setAvatar(final String avatar)
    {
        this.avatar = avatar;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(final String url)
    {
        this.url = url;
    }
}
