package com.atlassian.jira.plugins.dvcs.model;

import org.codehaus.jackson.annotate.JsonCreator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PullRequest implements IssueKeyedEntity
{
    private int id;
    private long remoteId;
    private int repositoryId;
    private String name;
    private String url;
    private PullRequestRef source;
    private PullRequestRef destination;
    private PullRequestStatus status;
    private Date createdOn;
    private Date updatedOn;
    private String author;
    private List<Participant> participants = new ArrayList<Participant>();
    private int commentCount;
    private List<Changeset> commits;
    private String executedBy;

    private List<String> issueKeys = new ArrayList<String>();

    @JsonCreator
    private PullRequest() {}

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

    public PullRequestStatus getStatus()
    {
        return status;
    }

    public void setStatus(final PullRequestStatus status)
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

    public List<Participant> getParticipants()
    {
        return participants;
    }

    public void setParticipants(final List<Participant> participants)
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

    public List<Changeset> getCommits()
    {
        return commits;
    }

    public void setCommits(final List<Changeset> commits)
    {
        this.commits = commits;
    }

    public String getExecutedBy()
    {
        return executedBy;
    }

    public void setExecutedBy(final String executedBy)
    {
        this.executedBy = executedBy;
    }

    public List<String> getIssueKeys()
    {
        return issueKeys;
    }

    public void setIssueKeys(final List<String> issueKeys)
    {
        this.issueKeys = issueKeys;
    }

}
