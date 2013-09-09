package com.atlassian.jira.plugins.dvcs.spi.github.service;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestComment;

/**
 * Provides {@link GitHubPullRequestComment} related services.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface GitHubPullRequestCommentService
{

    /**
     * Saves or updates provided {@link GitHubPullRequestComment}.
     * 
     * @param gitHubPullRequestComment
     *            to save/update
     */
    void save(GitHubPullRequestComment gitHubPullRequestComment);

    /**
     * Deletes provided {@link GitHubPullRequestComment}.
     * 
     * @param gitHubPullRequestComment
     *            to delete
     */
    void delete(GitHubPullRequestComment gitHubPullRequestComment);

    /**
     * @param id
     *            {@link GitHubPullRequestComment#getId()}
     * @return resolved {@link GitHubPullRequestComment}
     */
    GitHubPullRequestComment getById(int id);

    /**
     * @param gitHubId
     *            {@link GitHubPullRequestComment#getGitHubId()}
     * @return resolved {@link GitHubPullRequestComment}
     */
    GitHubPullRequestComment getByGitHubId(long gitHubId);

    /**
     * @param pullRequest
     *            {@link GitHubPullRequestComment#getPullRequest()}
     * @return resolved {@link GitHubPullRequestComment}-s
     */
    List<GitHubPullRequestComment> getByPullRequest(GitHubPullRequest pullRequest);

    /**
     * Synchronizes all comments and appropriate repository activities.
     * 
     * @param domainRepository
     *            for repository
     * @param pullRequest
     *            for which pull request
     * @param progress
     *            indicating that synchronization should be stopped
     */
    void synchronize(Repository domainRepository, GitHubPullRequest pullRequest, Progress progress);
}
