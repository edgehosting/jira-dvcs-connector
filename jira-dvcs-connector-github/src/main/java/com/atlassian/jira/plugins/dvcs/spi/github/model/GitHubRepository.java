package com.atlassian.jira.plugins.dvcs.spi.github.model;

/**
 * GitHub repository representation.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubRepository
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
     * @see #getName()
     */
    private String name;

    /**
     * @return {@link #getUrl()}
     */
    private String url;

    /**
     * Constructor.
     */
    public GitHubRepository()
    {
    }

    /**
     * @return Identity of the repository.
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
     * @return The name of the repository.
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name
     *            {@link #getName()}
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return URL of the repository.
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
