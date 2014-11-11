package com.atlassian.jira.plugins.dvcs.model.dev;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.gson.annotations.SerializedName;

/**
 *
 *
 */
@XmlRootElement
@XmlAccessorType (XmlAccessType.FIELD)
public class RestPullRequest
{
    private RestUser author;
    @SerializedName("createdOn")
    private long createdOn;
    @SerializedName("updatedOn")
    private long updatedOn;
    private long id;
    private String title;
    private String url;
    private String status;
    private RestRef source;
    private RestRef destination;
    private List<RestParticipant> participants;
    @SerializedName("commentCount")
    private int commentCount;
    private List<RestPrCommit> commits;

    public RestUser getAuthor()
    {
        return author;
    }

    public void setAuthor(final RestUser author)
    {
        this.author = author;
    }

    public long getCreatedOn()
    {
        return createdOn;
    }

    public void setCreatedOn(final long createdOn)
    {
        this.createdOn = createdOn;
    }

    public long getId()
    {
        return id;
    }

    public void setId(final long id)
    {
        this.id = id;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(final String title)
    {
        this.title = title;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(final String url)
    {
        this.url = url;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(final String status)
    {
        this.status = status;
    }

    public long getUpdatedOn()
    {
        return updatedOn;
    }

    public void setUpdatedOn(final long updatedOn)
    {
        this.updatedOn = updatedOn;
    }

    public RestRef getSource()
    {
        return source;
    }

    public void setSource(final RestRef source)
    {
        this.source = source;
    }

    public RestRef getDestination()
    {
        return destination;
    }

    public void setDestination(final RestRef destination)
    {
        this.destination = destination;
    }

    public List<RestParticipant> getParticipants()
    {
        return participants;
    }

    public void setParticipants(final List<RestParticipant> participants)
    {
        this.participants = participants;
    }

    public int getCommentCount()
    {
        return commentCount;
    }

    public void setCommentCount(final int commentCount)
    {
        this.commentCount = commentCount;
    }

    public List<RestPrCommit> getCommits()
    {
        return commits;
    }

    public void setCommits(final List<RestPrCommit> commits)
    {
        this.commits = commits;
    }
}

