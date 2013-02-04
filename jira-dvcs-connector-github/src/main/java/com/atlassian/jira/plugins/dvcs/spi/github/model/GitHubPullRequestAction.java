package com.atlassian.jira.plugins.dvcs.spi.github.model;

import java.util.Date;

/**
 * Action performed on the {@link GitHubPullRequest}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubPullRequestAction
{

    /**
     * Which action was performed on the {@link GitHubPullRequest}.
     * 
     * @author Stanislav Dvorscak
     * 
     */
    public enum Action
    {
        /**
         * Pull request was created/opened.
         */
        OPENED,

        /**
         * Pull request was closed - without merge.
         */
        CLOSED,

        /**
         * Pull request was reopened - only if it was already {@link #CLOSED}.
         */
        REOPENED,

        /**
         * Pull request was merged - it is final state.
         */
        MERGED

    }

    /**
     * @see #getId()
     */
    private int id;

    /**
     * @see #getCreatedBy()
     */
    private GitHubUser createdBy;

    /**
     * @see #getCreatedAt()
     */
    private Date createdAt;

    /**
     * @see #getAction()
     */
    private Action action;

    /**
     * @see #getGitHubEventId()
     */
    private String gitHubEventId;

    /**
     * Constructor.
     */
    public GitHubPullRequestAction()
    {
    }

    /**
     * @return Identity of the entity.
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
     * @return Actor of the {@link #getAction()}.
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
     * @return Date when {@link #getAction()} was performed.
     */
    public Date getCreatedAt()
    {
        return createdAt;
    }

    /**
     * @param createdAt
     *            {@link #getCreatedAt()}
     */
    public void setAt(Date createdAt)
    {
        this.createdAt = createdAt;
    }

    /**
     * @return Action which was performed - open, close, reopen, merge.
     */
    public Action getAction()
    {
        return action;
    }

    /**
     * @param action
     *            {@link #getAction()}
     */
    public void setAction(Action action)
    {
        this.action = action;
    }

    /**
     * @return GitHub Event ID - of this action - used during synchronization.
     */
    public String getGitHubEventId()
    {
        return gitHubEventId;
    }

    /**
     * @param gitHubEventId
     *            {@link #getGitHubEventId()}
     */
    public void setGitHubEventId(String gitHubEventId)
    {
        this.gitHubEventId = gitHubEventId;
    }

}
