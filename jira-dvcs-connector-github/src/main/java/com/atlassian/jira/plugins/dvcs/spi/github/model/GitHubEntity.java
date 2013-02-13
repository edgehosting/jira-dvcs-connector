package com.atlassian.jira.plugins.dvcs.spi.github.model;

/**
 * Entity base for the all GitHub related entities.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubEntity
{

    /**
     * @see #getId()
     */
    private int id;

    /**
     * @see #getRepository()
     */
    private GitHubRepository repository;

    /**
     * Constructor.
     */
    public GitHubEntity()
    {
    }

    /**
     * @return Identity of this entity.
     */
    public final int getId()
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
     * Over which repository is this entity instance.
     * 
     * @return repository
     */
    public GitHubRepository getRepository()
    {
        return repository;
    }

    /**
     * @param repository
     *            {@link #getRepository()}
     */
    public void setRepository(GitHubRepository repository)
    {
        this.repository = repository;
    }

}
