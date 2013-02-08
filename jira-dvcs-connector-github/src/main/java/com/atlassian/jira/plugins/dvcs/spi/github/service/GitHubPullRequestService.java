package com.atlassian.jira.plugins.dvcs.spi.github.service;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;

/**
 * Defines {@link GitHubPullRequest}'s related services.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface GitHubPullRequestService
{

    /**
     * Saves or updates provided {@link GitHubPullRequest}.
     * 
     * @param gitHubPullRequest
     *            to save/update
     */
    void save(GitHubPullRequest gitHubPullRequest);

    /**
     * Deletes provided {@link GitHubPullRequest}.
     * 
     * @param gitHubPullRequest
     */
    void delete(GitHubPullRequest gitHubPullRequest);

    /**
     * @param id
     *            {@link GitHubPullRequest#getId()}
     * @return resolved {@link GitHubPullRequest}
     */
    GitHubPullRequest getById(int id);

    /**
     * @param gitHubId
     *            {@link GitHubPullRequest#getGitHubId()}
     * @return resolved {@link GitHubPullRequest}
     */
    GitHubPullRequest getByGitHubId(long gitHubId);

    /**
     * @return all {@link GitHubPullRequest}-s
     */
    List<GitHubPullRequest> getAll();

    /**
     * @param repository
     *            owning pull request
     * @param gitHubId
     *            identity of the pull request
     * @param pullRequestNumber
     *            the number of the pull request
     * @return newly created or existing pull request
     */
    GitHubPullRequest fetch(Repository repository, long gitHubId, int pullRequestNumber);

}
