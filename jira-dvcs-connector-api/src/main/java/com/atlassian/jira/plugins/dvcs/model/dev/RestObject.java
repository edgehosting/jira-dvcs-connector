package com.atlassian.jira.plugins.dvcs.model.dev;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement (name = "object")
@XmlAccessorType (XmlAccessType.FIELD)
public class RestObject
{
    private RestRepository repository;
    private List<RestChangeset> commits;

    public RestObject()
    {
    }

    public RestObject(final RestRepository repository, final List<RestChangeset> commits)
    {
        this.repository = repository;
        this.commits = commits;
    }

    public RestRepository getRepository()
    {
        return repository;
    }

    public void setRepository(final RestRepository repository)
    {
        this.repository = repository;
    }

    public List<RestChangeset> getCommits()
    {
        return commits;
    }

    public void setCommits(final List<RestChangeset> commits)
    {
        this.commits = commits;
    }
}
