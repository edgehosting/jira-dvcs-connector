package com.atlassian.jira.plugins.dvcs.spi.github.service;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubEvent;

/**
 * Business layer of the {@link GitHubEvent}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface GitHubEventService
{

    /**
     * Saves or updates provided {@link GitHubEvent}.
     * 
     * @param gitHubEvent
     *            to save/update
     */
    void save(GitHubEvent gitHubEvent);

    /**
     * @param gitHubId {@link GitHubEvent#getId()}
     * @return {@link GitHubEvent}
     */
    GitHubEvent getByGitHubId(String gitHubId);

    /**
     * @return Returns last {@link GitHubEvent}.
     */
    GitHubEvent getLast();

    /**
     * @return Returns last {@link GitHubEvent#isSavePoint()}.
     */
    GitHubEvent getLastSavePoint();

}
