package com.atlassian.jira.plugins.dvcs.spi.github.service;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPush;

/**
 * Provides services related to the {@link GitHubPush}.
 * 
 * @author stanislav-dvorscak@solumiss.eu
 * 
 */
public interface GitHubPushService
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
