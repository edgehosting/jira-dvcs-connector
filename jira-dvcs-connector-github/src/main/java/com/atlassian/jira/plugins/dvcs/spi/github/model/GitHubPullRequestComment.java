package com.atlassian.jira.plugins.dvcs.spi.github.model;

import java.util.Date;

/**
 * Holds comment information done on the {@link GitHubPullRequest}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubPullRequestComment
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
     * @see #getPullRequest()
     */
    private GitHubPullRequest pullRequest;

    /**
     * @see #getCreatedAt()
     */
    private Date createdAt;

    /**
     * @see #getCreatedBy()
     */
    private GitHubUser createdBy;

    /**
     * @see #getText()
     */
    private String text;

    /**
     * Constructor.
     */
    public GitHubPullRequestComment()
    {
    }

    /**
     * @return Identity of this object.
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
     * @return Identity on the GitHub side.
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
     * @return To which pull request was done commenting.
     */
    public GitHubPullRequest getPullRequest()
    {
        return pullRequest;
    }

    /**
     * @param pullRequest
     *            {@link #getPullRequest()}
     */
    public void setPullRequest(GitHubPullRequest pullRequest)
    {
        this.pullRequest = pullRequest;
    }

    /**
     * @return Date of the comment creation.
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
     * @return User, whose creates this comment.
     */
    public GitHubUser getCreatedBy()
    {
        return createdBy;
    }

    /**
     * @param createdBy
     *            {@link #getCreatedBy()}
     */
    public void setCreatedBy(GitHubUser createdBy)
    {
        this.createdBy = createdBy;
    }

    /**
     * @return Text version of the comment.
     */
    public String getText()
    {
        return text;
    }

    /**
     * @param text
     *            {@link #getText()}
     */
    public void setText(String text)
    {
        this.text = text;
    }

}
