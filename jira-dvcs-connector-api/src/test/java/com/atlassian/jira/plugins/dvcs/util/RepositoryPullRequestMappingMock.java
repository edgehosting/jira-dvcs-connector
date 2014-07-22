package com.atlassian.jira.plugins.dvcs.util;

import com.atlassian.jira.plugins.dvcs.activity.PullRequestParticipantMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryCommitMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import net.java.ao.EntityManager;

import java.beans.PropertyChangeListener;
import java.util.Date;

public class RepositoryPullRequestMappingMock implements RepositoryPullRequestMapping
{
    private String author;
    private String executedBy;
    private String sourceBranch;
    private String destinationBranch;
    private Date updatedOn;
    private String name;
    private RepositoryCommitMapping[] commits = new RepositoryCommitMapping[0];

    @Override
    public Long getRemoteId()
    {
        return null;
    }

    @Override
    public int getToRepositoryId()
    {
        return 0;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getUrl()
    {
        return null;
    }

    @Override
    public String getSourceBranch()
    {
        return sourceBranch;
    }

    @Override
    public String getDestinationBranch()
    {
        return destinationBranch;
    }

    @Override
    public String getLastStatus()
    {
        return null;
    }

    @Override
    public Date getCreatedOn()
    {
        return null;
    }

    @Override
    public Date getUpdatedOn()
    {
        return updatedOn;
    }

    @Override
    public String getAuthor()
    {
        return author;
    }

    @Override
    public RepositoryCommitMapping[] getCommits()
    {
        return commits;
    }

    @Override
    public String getSourceRepo()
    {
        return null;
    }

    @Override
    public PullRequestParticipantMapping[] getParticipants()
    {
        return new PullRequestParticipantMapping[0];
    }

    @Override
    public int getCommentCount()
    {
        return 0;
    }

    @Override
    public String getExecutedBy()
    {
        return executedBy;
    }

    @Override
    public void setRemoteId(final Long id)
    {

    }

    @Override
    public void setToRepositoryId(final int repoId)
    {

    }

    @Override
    public void setName(final String name)
    {
        this.name = name;
    }

    @Override
    public void setUrl(final String url)
    {

    }

    @Override
    public void setSourceBranch(final String branch)
    {
        this.sourceBranch = branch;
    }

    @Override
    public void setDestinationBranch(final String branch)
    {
        this.destinationBranch = branch;
    }

    @Override
    public void setLastStatus(final String status)
    {

    }

    @Override
    public void setCreatedOn(final Date date)
    {

    }

    @Override
    public void setUpdatedOn(final Date date)
    {
        this.updatedOn = date;
    }

    @Override
    public void setAuthor(final String author)
    {
        this.author = author;
    }

    @Override
    public void setSourceRepo(final String sourceRepo)
    {

    }

    @Override
    public void setCommentCount(final int commentCount)
    {

    }

    @Override
    public void setExecutedBy(final String user)
    {
        this.executedBy = user;
    }

    @Override
    public int getDomainId()
    {
        return 0;
    }

    @Override
    public void setDomainId(final int domainId)
    {

    }

    @Override
    public int getID()
    {
        return 0;
    }

    @Override
    public void init()
    {

    }

    @Override
    public void save()
    {

    }

    @Override
    public EntityManager getEntityManager()
    {
        return null;
    }

    @Override
    public void addPropertyChangeListener(final PropertyChangeListener propertyChangeListener)
    {

    }

    @Override
    public void removePropertyChangeListener(final PropertyChangeListener propertyChangeListener)
    {

    }

    @Override
    public Class getEntityType()
    {
        return RepositoryPullRequestMapping.class;
    }

    public void setCommits(final RepositoryCommitMapping[] commits)
    {
        this.commits = commits;
    }
}
