package com.atlassian.jira.plugins.dvcs.spi.github.model;

import java.util.Date;

/**
 * Comment assigned to the {@link GitHubCommit}.
 * 
 * @author stanislav-dvorscak@solumiss.eu
 * 
 */
public class GitHubCommitComment
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
     * @see #getCreatedAt()
     */
    private Date createdAt;

    /**
     * @see #getCommit()
     */
    private GitHubCommit commit;

    /**
     * @see #getText()
     */
    private String text;

    /**
     * Constructor.
     */
    public GitHubCommitComment()
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
     * @return GitHub identity.
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
     * @return Date of creation of this comment.
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
     * @return For which commit is this comment.
     */
    public GitHubCommit getCommit()
    {
        return commit;
    }

    /**
     * @param commit
     *            {@link #getGitHubCommit()}
     */
    public void setCommit(GitHubCommit commit)
    {
        this.commit = commit;
    }

    /**
     * @return The text/comment assigned to the {@link #getGitHubCommit()}.
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
