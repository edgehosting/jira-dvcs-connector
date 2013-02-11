package com.atlassian.jira.plugins.dvcs.spi.github.model;

import com.atlassian.jira.plugins.dvcs.model.Repository;

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
     * @see #get
     */
    private int repositoryId;

    /**
     * @see #getGitHubId()
     */
    private long gitHubId;

    /**
     * @see #getName()
     */
    private String name;

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
     * @return {@link Repository#getId()}
     */
    public int getRepositoryId()
    {
        return repositoryId;
    }

    /**
     * @param repositoryId
     *            {@link #getRepositoryId()}
     */
    public void setRepositoryId(int repositoryId)
    {
        this.repositoryId = repositoryId;
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

}
