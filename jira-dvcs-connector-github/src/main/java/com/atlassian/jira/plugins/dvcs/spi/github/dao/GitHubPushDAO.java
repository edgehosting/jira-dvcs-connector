package com.atlassian.jira.plugins.dvcs.spi.github.dao;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPush;

/**
 * Provides {@link GitHubPush} DAO services.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface GitHubPushDAO
{

    /**
     * @param gitHubPush
     *            to save/update
     */
    void save(GitHubPush gitHubPush);

    /**
     * @param gitHubPush
     *            to delete
     */
    void delete(GitHubPush gitHubPush);

    /**
     * @param id
     *            {@link GitHubPush#getId()}
     * @return resolved {@link GitHubPush}
     */
    GitHubPush getById(int id);

    /**
     * @param sha
     *            {@link GitHubPush#getBefore()}
     * @return resolved {@link GitHubPush}
     */
    GitHubPush getByBefore(String sha);

    /**
     * @param sha
     *            {@link GitHubPush#getHead()}
     * @return resolved {@link GitHubPush}
     */
    GitHubPush getByHead(String sha);

}
