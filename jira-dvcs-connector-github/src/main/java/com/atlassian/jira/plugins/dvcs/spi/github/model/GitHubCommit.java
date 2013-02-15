package com.atlassian.jira.plugins.dvcs.spi.github.model;

import java.util.Date;

/**
 * Model of the commit.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubCommit extends GitHubEntity
{

    /**
     * @see #getRepository()
     */
    private GitHubRepository repository;

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
     * @see #getCreatedByName()
     */
    private String createdByName;

    /**
     * @see #getCreatedByAvatarUrl()
     */
    private String createdByAvatarUrl;

    /**
     * @see #getHtmlUrl()
     */
    private String htmlUrl;

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
     * @return Repository owner of this commit.
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
     * @return Display name of the {@link #getCreatedBy()}.
     */
    public String getCreatedByName()
    {
        return createdByName;
    }

    /**
     * @param createdByName
     *            {@link #getCreatedByName()}
     */
    public void setCreatedByName(String createdByName)
    {
        this.createdByName = createdByName;
    }

    /**
     * @return Avatar URL of the {@link #getCreatedBy()}.
     */
    public String getCreatedByAvatarUrl()
    {
        return createdByAvatarUrl;
    }

    /**
     * @param createdByAvatarUrl
     *            {@link #getCreatedByAvatarUrl()}
     */
    public void setCreatedByAvatarUrl(String createdByAvatarUrl)
    {
        this.createdByAvatarUrl = createdByAvatarUrl;
    }

    /**
     * @return HTML URL of this commit.
     */
    public String getHtmlUrl()
    {
        return htmlUrl;
    }

    /**
     * @param htmlUrl
     *            {@link #getHtmlUrl()}
     */
    public void setHtmlUrl(String htmlUrl)
    {
        this.htmlUrl = htmlUrl;
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
