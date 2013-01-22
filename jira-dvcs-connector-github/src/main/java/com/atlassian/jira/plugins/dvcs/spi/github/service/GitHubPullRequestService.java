package com.atlassian.jira.plugins.dvcs.spi.github.service;

import java.util.List;

import org.eclipse.egit.github.core.PullRequest;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;

/**
 * Defines {@link GitHubPullRequest}'s related services.
 * 
 * @author stanislav-dvorscak@solumiss.eu
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
     * @param issueKey
     *            for which are resolved {@link GitHubPullRequest}-s
     * @return resolved {@link GitHubPullRequest}-s
     */
    List<GitHubPullRequest> getGitHubPullRequest(String issueKey);

    /**
     * Re-maps {@link PullRequest} to the {@link GitHubPullRequest}.
     * 
     * @param target
     *            internal model
     * @param source
     *            egit model
     */
    void map(GitHubPullRequest target, PullRequest source);

}
