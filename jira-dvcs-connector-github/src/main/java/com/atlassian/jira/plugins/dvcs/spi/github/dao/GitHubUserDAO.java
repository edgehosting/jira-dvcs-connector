package com.atlassian.jira.plugins.dvcs.spi.github.dao;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubUser;

/**
 * The {@link GitHubUser} related services.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface GitHubUserDAO
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
     * @param id
     *            {@link GitHubUser#getId()}
     * @return resolved {@link GitHubUser}
     */
    GitHubUser getById(int id);

    /**
     * @param login
     *            {@link GitHubUser#getLogin()}
     * @return resolved {@link GitHubUser}
     */
    GitHubUser getByLogin(String login);

}
