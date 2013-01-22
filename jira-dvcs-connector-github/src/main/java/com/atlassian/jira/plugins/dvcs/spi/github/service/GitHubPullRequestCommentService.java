package com.atlassian.jira.plugins.dvcs.spi.github.service;

import org.eclipse.egit.github.core.CommitComment;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestComment;

/**
 * Provides {@link GitHubPullRequestComment} related services.
 * 
 * @author stanislav-dvorscak@solumiss.eu
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
     * Re-maps egit model into the internal model.
     * 
     * @param target
     *            internal model
     * @param source
     *            egit model
     * @param pullRequest
     *            {@link GitHubPullRequestComment#getPullRequest()}
     */
    void map(GitHubPullRequestComment target, CommitComment source, GitHubPullRequest pullRequest);

}
