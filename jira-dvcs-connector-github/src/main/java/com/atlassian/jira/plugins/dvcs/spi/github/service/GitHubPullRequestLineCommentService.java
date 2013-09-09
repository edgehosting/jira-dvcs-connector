package com.atlassian.jira.plugins.dvcs.spi.github.service;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestLineComment;

/**
 * The {@link GitHubPullRequestLineComment} related services.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface GitHubPullRequestLineCommentService
{

    /**
     * Saves or updates provided {@link GitHubPullRequestLineComment}.
     * 
     * @param gitHubPullRequestLineComment
     *            to save/update
     */
    void save(GitHubPullRequestLineComment gitHubPullRequestLineComment);

    /**
     * Deletes provided {@link GitHubPullRequestLineComment}.
     * 
     * @param gitHubPullRequestLineComment
     *            to delete
     */
    void delete(GitHubPullRequestLineComment gitHubPullRequestLineComment);

    /**
     * @param id
     *            {@link GitHubPullRequestLineComment#getId()}
     * @return resolved {@link GitHubPullRequestLineComment}
     */
    GitHubPullRequestLineComment getById(int id);

    /**
     * @param gitHubId
     *            {@link GitHubPullRequestLineComment#getGitHubId()}
     * @return {@link GitHubPullRequestLineComment}
     */
    GitHubPullRequestLineComment getByGitHubId(long gitHubId);

    /**
     * @param pullRequest
     *            {@link GitHubPullRequestLineComment#getPullRequest()}
     * @return resolved {@link GitHubPullRequestLineComment}-s
     */
    List<GitHubPullRequestLineComment> getByPullRequest(GitHubPullRequest pullRequest);

    /**
     * Synchronizes all comments and appropriate repository activities.
     * 
     * @param domainRepository
     *            for repository
     * @param pullRequest
     *            for which pull request will be synchronized comments
     * @param progress
     *            indicating that synchronization should be stopped
     */
    void synchronize(Repository domainRepository, GitHubPullRequest pullRequest, Progress progress);

}
