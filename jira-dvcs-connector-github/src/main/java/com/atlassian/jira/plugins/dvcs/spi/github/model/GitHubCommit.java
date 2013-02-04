package com.atlassian.jira.plugins.dvcs.spi.github.model;

import java.util.Date;

/**
 * Model of the commit.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubCommit
{

    /**
     * @see #getId()
     */
    private int id;

    /**
     * @see #getSha()
     */
    private String sha;

    /**
     * @see #getCreatedAt()
     */
    private Date createdAt;

    /**
     * @see #getCreatedBy()
     */
    private String createdBy;

    /**
     * @see #getMessage()
     */
    private String message;

    /**
     * Constructor.
     */
    public GitHubCommit()
    {
    }

    /**
     * @return Identity of the commit.
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
     * @return SHA id of the GitHub/Git commit.
     */
    public String getSha()
    {
        return sha;
    }

    /**
     * @param sha
     *            {@link #getSha()}
     */
    public void setSha(String sha)
    {
        this.sha = sha;
    }

    /**
     * @return The date when the commit was introduced.
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
     * @return Author of the commit.
     */
    public String getCreatedBy()
    {
        return createdBy;
    }

    /**
     * @param createdBy
     *            {@link #getCreatedBy()}
     */
    public void setCreatedBy(String createdBy)
    {
        this.createdBy = createdBy;
    }

    /**
     * @return A message related to the commit.
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * @param message
     *            {@link #getMessage()}
     */
    public void setMessage(String message)
    {
        this.message = message;
    }

}
