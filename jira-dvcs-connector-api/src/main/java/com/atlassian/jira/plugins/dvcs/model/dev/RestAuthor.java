package com.atlassian.jira.plugins.dvcs.model.dev;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType (XmlAccessType.FIELD)
public class RestAuthor
{
    private String name;
    private String emailAddress;

    public RestAuthor()
    {
    }

    public RestAuthor(final String name, final String emailAddress)
    {
        this.name = name;
        this.emailAddress = emailAddress;
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
}
