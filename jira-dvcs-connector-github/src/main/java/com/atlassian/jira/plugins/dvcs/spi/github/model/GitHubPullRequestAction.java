package com.atlassian.jira.plugins.dvcs.spi.github.model;

import java.util.Date;

/**
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
     * @see #getActor()
     */
    private GitHubUser actor;

    /**
     * @see #getAt()
     */
    private Date at;

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
     * @return Actor of the {@link #getAction()}.
     */
    public GitHubUser getActor()
    {
        return actor;
    }

    /**
     * @param actor
     *            {@link #getActor()}
     */
    public void setActor(GitHubUser actor)
    {
        this.actor = actor;
    }

    /**
     * @return Date when {@link #getAction()} was performed.
     */
    public Date getAt()
    {
        return at;
    }

    /**
     * @param at
     *            {@link #getAt()}
     */
    public void setAt(Date at)
    {
        this.at = at;
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
