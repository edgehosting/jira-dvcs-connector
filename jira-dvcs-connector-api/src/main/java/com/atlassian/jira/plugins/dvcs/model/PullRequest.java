package com.atlassian.jira.plugins.dvcs.model;

import java.util.Date;

public class PullRequest
{
    private int id;
    private long remoteId;
    private int repositoryId;
    private String name;
    private String url;
    private PullRequestRef source;
    private PullRequestRef destination;
    private String status;
    private Date createdOn;
    private Date updatedOn;
    private String author;

    public PullRequest(final int id)
    {
        this.id = id;
    }

    public int getId()
    {
        return id;
    }

    public void setId(final int id)
    {
        this.id = id;
    }

    public long getRemoteId()
    {
        return remoteId;
    }

    public void setRemoteId(final long remoteId)
    {
        this.remoteId = remoteId;
    }

    public int getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId(final int repositoryId)
    {
        this.repositoryId = repositoryId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(final String name)
    {
        this.name = name;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(final String url)
    {
        this.url = url;
    }

    public PullRequestRef getSource()
    {
        return source;
    }

    public void setSource(final PullRequestRef source)
    {
        this.source = source;
    }

    public PullRequestRef getDestination()
    {
        return destination;
    }

    public void setDestination(final PullRequestRef destination)
    {
        this.destination = destination;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(final String status)
    {
        this.status = status;
    }

    public Date getCreatedOn()
    {
        return createdOn;
    }

    public void setCreatedOn(final Date createdOn)
    {
        this.createdOn = createdOn;
    }

    public String getAuthor()
    {
        return author;
    }

    public void setAuthor(final String author)
    {
        this.author = author;
    }

    public Date getUpdatedOn()
    {
        return updatedOn;
    }

    public void setUpdatedOn(final Date updatedOn)
    {
        this.updatedOn = updatedOn;
    }
}
