package com.atlassian.jira.plugins.dvcs.spi.github.dao;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;

/**
 * Provides DAO services related to an {@link GitHubPullRequest}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface GitHubPullRequestDAO
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

}
