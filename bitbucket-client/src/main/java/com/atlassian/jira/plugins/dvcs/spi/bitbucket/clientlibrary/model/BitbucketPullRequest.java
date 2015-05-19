package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class BitbucketPullRequest implements Serializable
{
    private static final long serialVersionUID = 846355472211323786L;

    private Long id;
    private String title;
    private String description;
    private String state;
    private BitbucketAccount user;
    private BitbucketAccount author;
    private BitbucketAccount closedBy;
    private BitbucketLinks links;
    private BitbucketPullRequestHead source;
    private BitbucketPullRequestHead destination;
    private Date createdOn;
    private Date updatedOn;
    private List<BitbucketPullRequestParticipant> participants;
    private List<BitbucketUser> reviewers;

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

    public BitbucketLinks getLinks()
    {
        return links;
    }

    public void setLinks(BitbucketLinks links)
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

    public String getState()
    {
        return state;
    }

    public void setState(String state)
    {
        this.state = state;
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

    public List<BitbucketPullRequestParticipant> getParticipants()
    {
        return participants;
    }

    public void setParticipants(final List<BitbucketPullRequestParticipant> participants)
    {
        this.participants = participants;
    }

    public BitbucketAccount getAuthor()
    {
        return author;
    }

    public void setAuthor(final BitbucketAccount author)
    {
        this.author = author;
    }

    public List<BitbucketUser> getReviewers()
    {
        return reviewers;
    }

    public void setReviewers(final List<BitbucketUser> reviewers)
    {
        this.reviewers = reviewers;
    }

    public BitbucketAccount getClosedBy()
    {
        return closedBy;
    }

    public void setClosedBy(final BitbucketAccount closedBy)
    {
        this.closedBy = closedBy;
    }
}
