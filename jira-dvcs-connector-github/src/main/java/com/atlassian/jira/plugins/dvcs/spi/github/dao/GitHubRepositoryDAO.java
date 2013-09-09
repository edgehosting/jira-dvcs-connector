package com.atlassian.jira.plugins.dvcs.spi.github.dao;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;

/**
 * DAO layer over {@link GitHubRepository}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface GitHubRepositoryDAO
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

}
