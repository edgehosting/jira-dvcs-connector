package com.atlassian.jira.plugins.dvcs.model.dev;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType (XmlAccessType.FIELD)
public class RestAuthor
{
    private String avatar;
    private String emailAddress;
    private String name;

    public RestAuthor()
    {
    }

    public RestAuthor(final String name, final String emailAddress, final String avatar)
    {
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
}
