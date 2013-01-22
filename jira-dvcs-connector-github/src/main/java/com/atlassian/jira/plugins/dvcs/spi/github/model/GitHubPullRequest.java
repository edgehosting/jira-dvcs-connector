package com.atlassian.jira.plugins.dvcs.spi.github.model;

import java.util.Date;

/**
 * A Pull Request.
 * 
 * @author stanislav-dvorscak@solumiss.eu
 * 
 */
public class GitHubPullRequest
{

    /**
     * @see #getId()
     */
    private int id;

    /**
     * @see #getGitHubId()
     */
    private long gitHubId;

    /**
     * @see #getTitle()
     */
    private String title;

    /**
     * @see #get
     */
    private Date createdAt;

    /**
     * Constructor.
     */
    public GitHubPullRequest()
    {
    }

    /**
     * @return The identity of the {@link GitHubPullRequest}.
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
     * @return GitHub identity of object.
     */
    public long getGitHubId()
    {
        return gitHubId;
    }

    /**
     * @param gitHubId
     *            {@link #getGitHubId()}
     */
    public void setGitHubId(long gitHubId)
    {
        this.gitHubId = gitHubId;
    }

    /**
     * @return A title of this pull request.
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * @param title
     *            {@link #getTitle()}
     */
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * @return The date of the pull request creation.
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

}
