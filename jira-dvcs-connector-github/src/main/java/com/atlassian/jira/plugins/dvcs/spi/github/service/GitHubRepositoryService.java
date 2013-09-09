package com.atlassian.jira.plugins.dvcs.spi.github.service;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;

/**
 * Service layer over {@link GitHubRepository}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface GitHubRepositoryService
{

    /**
     * Saves or updates provided {@link GitHubRepository}.
     * 
     * @param gitHubRepository
     *            to save/update
     */
    void save(GitHubRepository gitHubRepository);

    /**
     * Deletes provided {@link GitHubRepository}.
     * 
     * @param gitHubRepository
     *            to delete
     */
    void delete(GitHubRepository gitHubRepository);

    /**
     * @param gitHubId
     *            {@link GitHubRepository#getGitHubId()}
     * @return resolved {@link GitHubRepository}
     */
    GitHubRepository getByGitHubId(long gitHubId);

    /**
     * @param domainRepository
     *            domain repository
     * @param owner
     *            of the repository - username/organization name
     * @param name
     *            of the repository
     * @param gitHubId
     *            if it is known - if it is not provided, means equals to zero, it will be still refreshed
     * @return creates new or returns existing {@link GitHubRepository}
     */
    GitHubRepository fetch(Repository domainRepository, String owner, String name, long gitHubId);

}
