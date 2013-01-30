package com.atlassian.jira.plugins.dvcs.spi.github.service;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubUser;

/**
 * The {@link GitHubUser} related services.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface GitHubUserService
{

    /**
     * Saves or updates provided user.
     * 
     * @param gitHubUser
     *            to save/update
     */
    void save(GitHubUser gitHubUser);

    /**
     * Deletes provided user.
     * 
     * @param gitHubUser
     *            to delete
     */
    void delete(GitHubUser gitHubUser);

    /**
     * @param login
     *            {@link GitHubUser#getLogin()}
     * @return resolved {@link GitHubUser}
     */
    GitHubUser getByLogin(String login);

    /**
     * @param login
     *            {@link GitHubUser#getLogin()}
     * @param repository
     *            over which repository
     * @return newly created or refreshed user
     */
    GitHubUser synchronize(String login, Repository repository);

}
