package com.atlassian.jira.plugins.dvcs.model.dev;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 *
 */
@XmlRootElement
@XmlAccessorType (XmlAccessType.FIELD)
public class RestRef
{
    private String branch;
    private String repository;
    private String url;

    public String getBranch()
    {
        return branch;
    }

    public void setBranch(final String branch)
    {
        this.branch = branch;
    }

    public String getRepository()
    {
        return repository;
    }

    public void setRepository(final String repository)
    {
        this.repository = repository;
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
