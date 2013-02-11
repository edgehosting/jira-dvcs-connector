package com.atlassian.jira.plugins.dvcs.spi.github.dao;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubEvent;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;

/**
 * DAO layer of the {@link GitHubEvent}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface GitHubEventDAO
{

    /**
     * Saves or updates provided {@link GitHubEvent}.
     * 
     * @param gitHubEvent
     *            to save/update
     */
    void save(GitHubEvent gitHubEvent);

    /**
     * @param gitHubId
     *            {@link GitHubEvent#getId()}
     * @return {@link GitHubEvent}
     */
    GitHubEvent getByGitHubId(String gitHubId);

    /**
     * @param gitHubRepository
     *            over which repository
     * @return Returns last {@link GitHubEvent}.
     */
    GitHubEvent getLast(GitHubRepository gitHubRepository);

    /**
     * @param giHubRepository
     *            over which repository
     * @return Returns last {@link GitHubEvent#isSavePoint()}.
     */
    GitHubEvent getLastSavePoint(GitHubRepository giHubRepositoryMapping);

}
