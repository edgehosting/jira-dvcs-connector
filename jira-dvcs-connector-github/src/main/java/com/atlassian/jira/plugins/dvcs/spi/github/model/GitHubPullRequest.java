package com.atlassian.jira.plugins.dvcs.spi.github.model;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * A Pull Request.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubPullRequest
{

    /**
     * @see #getId()
     */
    private int id;

    /**
     * @see #getSynchronizedAt()
     */
    private Date synchronizedAt;

    /**
     * @see #getGitHubId()
     */
    private long gitHubId;

    /**
     * @see #getTitle()
     */
    private String title;

    /**
     * @see #getActions()
     */
    private List<GitHubPullRequestAction> actions = new LinkedList<GitHubPullRequestAction>();

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
     * @return date when was last synchronized.
     */
    public Date getSynchronizedAt()
    {
        return synchronizedAt;
    }

    /**
     * @param synchronizedAt
     *            {@link #setSynchronizedAt(Date)}
     */
    public void setSynchronizedAt(Date synchronizedAt)
    {
        this.synchronizedAt = synchronizedAt;
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
     * @return The actions performed on this {@link GitHubPullRequest}.
     */
    public List<GitHubPullRequestAction> getActions()
    {
        return actions;
    }

    /**
     * @param actions
     *            {@link #getActions()}
     */
    public void setActions(List<GitHubPullRequestAction> actions)
    {
        this.actions = actions;
    }

}
