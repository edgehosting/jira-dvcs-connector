package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;
import java.util.Date;

/**
 * BitbucketPullRequest
 *
 *
 * <br />
 * <br />
 * Created on 11.12.2012, 14:02:57 <br />
 * <br />
 *
 * @author jhocman@atlassian.com
 *
 */
public class BitbucketPullRequest implements Serializable
{
    private static final long serialVersionUID = 846355472211323786L;

    private Long id;
    private String title;
    private String description;
    private String status;
    private BitbucketAccount user;
    private BitbucketPullRequestLinks links;
    private BitbucketPullRequestHead source;
    private BitbucketPullRequestHead destination;
    private Date createdOn;
    private Date updatedOn;

    public BitbucketPullRequest()
    {
        super();
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public BitbucketAccount getUser()
    {
        return user;
    }

    public void setUser(BitbucketAccount user)
    {
        this.user = user;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public BitbucketPullRequestLinks getLinks()
    {
        return links;
    }

    public void setLinks(BitbucketPullRequestLinks links)
    {
        this.links = links;
    }

    public BitbucketPullRequestHead getSource()
    {
        return source;
    }

    public void setSource(BitbucketPullRequestHead source)
    {
        this.source = source;
    }

    public BitbucketPullRequestHead getDestination()
    {
        return destination;
    }

    public void setDestination(BitbucketPullRequestHead destination)
    {
        this.destination = destination;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Date getCreatedOn()
    {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn)
    {
        this.createdOn = createdOn;
    }

    public Date getUpdatedOn()
    {
        return updatedOn;
    }

    public void setUpdatedOn(Date updatedOn)
    {
        this.updatedOn = updatedOn;
    }
}
