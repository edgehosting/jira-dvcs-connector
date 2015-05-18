package com.atlassian.jira.plugins.dvcs.dao;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.GitHubEventMapping;
import com.atlassian.jira.plugins.dvcs.model.Repository;

import java.util.Map;

/**
 * DAO layer of the {@link GitHubEventMapping}.
 *
 * @author Stanislav Dvorscak
 *
 */
public interface GitHubEventDAO
{

    /**
     * @param gitHubEvent
     *            values for creation - initial values of {@link GitHubEventMapping} entity
     * @return creates new GitHub even entry
     */
    GitHubEventMapping create(Map<String, Object> gitHubEvent);

    /**
     * Marks provided {@link GitHubEventMapping} as a save point - {@link GitHubEventMapping#isSavePoint()} == true.
     *
     * @param gitHubEvent
     *            for marking
     */
    void markAsSavePoint(GitHubEventMapping gitHubEvent);

    /**
     * Removes all events for provided repository.
     *
     * @param repository
     *            for which repository
     */
    void removeAll(Repository repository);

    /**
     * @param repository
     * @param gitHubId {@link GitHubEventMapping#getGitHubId()}
     * @return resolved {@link GitHubEventMapping} by remote id
     */
    GitHubEventMapping getByGitHubId(Repository repository, String gitHubId);

    /**
     * @param repository
     *            over which repository
     * @return Returns last {@link GitHubEventMapping}, which was marked as a save point - {@link GitHubEventMapping#isSavePoint()} == true.
     */
    GitHubEventMapping getLastSavePoint(Repository repository);

}
