package com.atlassian.jira.plugins.dvcs.spi.github.model;

import java.util.LinkedList;
import java.util.List;

/**
 * A Pull Request.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubPullRequest extends GitHubEntity
{

    /**
     * @see #getBaseRepository()
     */
    private GitHubRepository baseRepository;

    /**
     * @see #getGitHubId()
     */
    private long gitHubId;

    /**
     * @see #getTitle()
     */
    private String title;

    /**
     * @see #getUrl()
     */
    private String url;

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
     * @return Base repository on which will be this pull request applied.
     */
    public GitHubRepository getBaseRepository()
    {
        return baseRepository;
    }

    /**
     * @param baseRepository
     *            {@link #getBaseRepository()}
     */
    public void setBaseRepository(GitHubRepository baseRepository)
    {
        this.baseRepository = baseRepository;
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

    /**
     * @return URL of repository
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * @param url
     *            {@link #getUrl()}
     */
    public void setUrl(String url)
    {
        this.url = url;
    }

}
