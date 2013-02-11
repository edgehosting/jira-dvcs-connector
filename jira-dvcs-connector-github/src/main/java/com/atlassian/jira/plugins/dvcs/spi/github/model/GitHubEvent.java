package com.atlassian.jira.plugins.dvcs.spi.github.model;

import java.util.Date;

/**
 * Model of the GitHub event.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubEvent
{

    /**
     * @see #getId()
     */
    private int id;

    /**
     * @see #getGitHubId()
     */
    private String gitHubId;

    /**
     * @see #getRepository()
     */
    private GitHubRepository repository;

    /**
     * @see #getCreatedAt()
     */
    private Date createdAt;

    /**
     * @see #isSavePoint()
     */
    private boolean savePoint;

    /**
     * Constructor.
     */
    public GitHubEvent()
    {
    }

    /**
     * @return Identity of this entity.
     */
    public int getId()
    {
        return id;
    }

    /**
     * @param id
     *            {@link #getId()}
     */
    public void setId(int id)
    {
        this.id = id;
    }

    /**
     * @return GitHub identity.
     */
    public String getGitHubId()
    {
        return gitHubId;
    }

    /**
     * @return Repository owner of the event.
     */
    public GitHubRepository getRepository()
    {
        return repository;
    }

    /**
     * @param repository
     *            {@link #getRepository()}
     */
    public void setRepository(GitHubRepository repository)
    {
        this.repository = repository;
    }

    /**
     * @param gitHubId
     */
    public void setGitHubId(String gitHubId)
    {
        this.gitHubId = gitHubId;
    }

    /**
     * @return Date when the event was created.
     */
    public Date getCreatedAt()
    {
        return createdAt;
    }

    /**
     * @param createdAt
     *            {@link #getCreatedAt()}
     */
    public void setCreatedAt(Date createdAt)
    {
        this.createdAt = createdAt;
    }

    /**
     * @return Point of the last full synchronization.
     */
    public boolean isSavePoint()
    {
        return savePoint;
    }

    /**
     * @param savePoint
     *            {@link #isSavePoint()}
     */
    public void setSavePoint(boolean savePoint)
    {
        this.savePoint = savePoint;
    }

}
