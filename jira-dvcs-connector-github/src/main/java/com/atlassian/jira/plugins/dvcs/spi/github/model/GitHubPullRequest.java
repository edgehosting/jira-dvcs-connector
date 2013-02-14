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
     * @see #getBaseSha()
     */
    private String baseSha;

    /**
     * @see #getHeadRepository()
     */
    private GitHubRepository headRepository;

    /**
     * @see #getHeadSha()
     */
    private String headSha;

    /**
     * @see #getGitHubId()
     */
    private long gitHubId;

    /**
     * @see #getNumber()
     */
    private int number;

    /**
     * @see #getTitle()
     */
    private String title;

    /**
     * @see #getText()
     */
    private String text;

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
     * @return Base SHA.
     */
    public String getBaseSha()
    {
        return baseSha;
    }

    /**
     * @param baseSha
     *            {@link #getBaseSha()}
     */
    public void setBaseSha(String baseSha)
    {
        this.baseSha = baseSha;
    }

    /**
     * @return Head repository on which will be this pull request applied.
     */
    public GitHubRepository getHeadRepository()
    {
        return headRepository;
    }

    /**
     * @param headRepository
     *            {@link #getHeadRepository()}
     */
    public void setHeadRepository(GitHubRepository headRepository)
    {
        this.headRepository = headRepository;
    }

    /**
     * @return Current SHA of the head.
     */
    public String getHeadSha()
    {
        return headSha;
    }

    /**
     * @param headSha
     *            {@link #getHeadSha()}
     */
    public void setHeadSha(String headSha)
    {
        this.headSha = headSha;
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
     * @return PullRequest number.
     */
    public int getNumber()
    {
        return number;
    }

    /**
     * @param number
     *            {@link #getNumber()}
     */
    public void setNumber(int number)
    {
        this.number = number;
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
     * @return Message/text assigned to the pull request.
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
