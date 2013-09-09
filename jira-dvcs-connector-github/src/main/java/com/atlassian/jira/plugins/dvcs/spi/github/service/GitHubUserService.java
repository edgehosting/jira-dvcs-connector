package com.atlassian.jira.plugins.dvcs.spi.github.service;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
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
     * 
     * @param domainRepository
     *            {@link GitHubUser#getDomain()}
     * @param domain
     *            for repository
     * @param login
     *            {@link GitHubUser#getLogin()}
     * @return fetch the {@link GitHubUser} from the database, if it was already synchronized or synchronizes it
     */
    GitHubUser fetch(Repository domainRepository, GitHubRepository domain, String login);

}
