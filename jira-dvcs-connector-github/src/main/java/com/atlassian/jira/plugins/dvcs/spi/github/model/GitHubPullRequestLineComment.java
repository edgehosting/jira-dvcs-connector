package com.atlassian.jira.plugins.dvcs.spi.github.model;

import java.util.Date;

/**
 * Line comment over an {@link GitHubCommit}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubPullRequestLineComment
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
     * @see #getCommit()
     */
    private GitHubCommit commit;

    /**
     * @see #getPath()
     */
    private String path;

    /**
     * @see #getLine()
     */
    private int line;

    /**
     * @see #getText()
     */
    private String text;

    /**
     * Constructor.
     */
    public GitHubPullRequestLineComment()
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
     * @return GitHub side identity.
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
     * @return On which {@link GitHubPullRequest} is done this comment.
     */
    public GitHubPullRequest getPullRequest()
    {
        return pullRequest;
    }

    /**
     * @param pullRequest
     *            {@link #getPullRequest()},
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
     * @return Creator of the comment.
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
     * @return On which commit was done this comment.
     */
    public GitHubCommit getCommit()
    {
        return commit;
    }

    /**
     * @param commit
     *            {@link #getCommit()}
     */
    public void setCommit(GitHubCommit commit)
    {
        this.commit = commit;
    }

    /**
     * @return The path to the file.
     */
    public String getPath()
    {
        return path;
    }

    /**
     * @param path
     *            {@link #getPath()}
     */
    public void setPath(String path)
    {
        this.path = path;
    }

    /**
     * @return The number of the line of the code.
     */
    public int getLine()
    {
        return line;
    }

    /**
     * @param line
     *            {@link #getLine()}
     */
    public void setLine(int line)
    {
        this.line = line;
    }

    /**
     * @return Raw text of the comment.
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
