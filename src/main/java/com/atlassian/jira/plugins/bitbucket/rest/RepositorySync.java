package com.atlassian.jira.plugins.bitbucket.rest;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "sync")
@XmlAccessorType(XmlAccessType.FIELD)
public class RepositorySync
{
    @XmlAttribute
    private String projectKey;

    @XmlAttribute
    private String url;

    @XmlAttribute
    private String message;

    public RepositorySync()
    {
    }

    public RepositorySync(String projectKey, String url, String message)
    {
        this.projectKey = projectKey;
        this.url = url;
        this.message = message;
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

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }
}
