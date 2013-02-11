package com.atlassian.jira.plugins.dvcs.spi.github.service;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;

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
     * @param repository
     *            {@link GitHubPullRequestMapping#getRepository()}
     * @return resolved {@link GitHubPullRequest}-s
     */
    List<GitHubPullRequest> getByRepository(GitHubRepository repository);

    /**
     * @param gitHubRepository
     *            for which repository it is loaded
     * @param gitHubId
     *            identity of the pull request
     * @param pullRequestNumber
     *            the number of the pull request
     * @param repository
     *            owning pull request
     * @return newly created or existing pull request
     */
    GitHubPullRequest fetch(GitHubRepository gitHubRepository, long gitHubId, int pullRequestNumber, Repository repository);

}
