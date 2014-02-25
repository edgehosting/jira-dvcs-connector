package com.atlassian.jira.plugins.dvcs.github.api;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.github.api.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.github.api.model.GitHubRepositoryHook;
import com.atlassian.jira.plugins.dvcs.model.Repository;

/**
 * API abstraction over GitHub REST API.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface GitHubRESTClient
{

    /**
     * @param repository
     *            on which repository
     * @param hook
     *            for creation
     * @return created hook
     */
    GitHubRepositoryHook addHook(Repository repository, GitHubRepositoryHook hook);

    /**
     * @param repository
     *            on which repository
     * @param hook
     *            for deletion
     */
    void deleteHook(Repository repository, GitHubRepositoryHook hook);

    /**
     * @param repository
     *            for which repository
     * @return returns hooks for provided repository.
     */
    List<GitHubRepositoryHook> getHooks(Repository repository);

    /**
     * Returns {@link GitHubPullRequest} for provided repository and its number.
     * 
     * @param repository
     * @param number
     * @return resolved {@link GitHubPullRequest}
     */
    GitHubPullRequest getPullRequest(Repository repository, int number);
}
