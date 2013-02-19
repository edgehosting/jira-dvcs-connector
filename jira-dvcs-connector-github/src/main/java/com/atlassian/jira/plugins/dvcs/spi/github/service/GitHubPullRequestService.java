package com.atlassian.jira.plugins.dvcs.spi.github.service;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestAction;
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
     *            {@link GitHubPullRequestMapping#getDomain()}
     * @return resolved {@link GitHubPullRequest}-s
     */
    List<GitHubPullRequest> getByRepository(GitHubRepository repository);

    /**
     * @param pullRequest
     * @return resolves {@link GitHubPullRequestAction.Action#OPENED}
     * @throws IllegalStateException
     *             if pull request does not have any opened action
     */
    GitHubPullRequestAction getOpenAction(GitHubPullRequest pullRequest);

    /**
     * @param domainRepository
     *            for which repository
     * @param domain
     *            for which repository
     * @param gitHubId
     *            {@link GitHubPullRequest#getGitHubId()}
     * @param pullRequestNumber
     *            {@link GitHubPullRequest#getNumber()}
     * @return newly created or existing pull request
     */
    GitHubPullRequest fetch(Repository domainRepository, GitHubRepository domain, long gitHubId, int pullRequestNumber);

    /**
     * Synchronizes all GitHub pull requests and appropriates pull request activities.
     * 
     * @param domainRepository
     *            for repository
     * @param domain
     *            for repository
     */
    void synchronize(Repository domainRepository, GitHubRepository domain);

}
