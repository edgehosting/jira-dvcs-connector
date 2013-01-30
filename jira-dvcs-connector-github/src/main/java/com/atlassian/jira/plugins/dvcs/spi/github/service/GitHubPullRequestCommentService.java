package com.atlassian.jira.plugins.dvcs.spi.github.service;

import java.util.List;

import org.eclipse.egit.github.core.Comment;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubUser;

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
     * @param issueKey
     *            key of the linked issue
     * @return resolved {@link GitHubPullRequestComment}-s
     */
    List<GitHubPullRequestComment> getByIssueKey(String issueKey);

    /**
     * Re-maps egit model into the internal model.
     * 
     * @param target
     *            internal model
     * @param comment
     *            egit model
     * @param pullRequest
     *            {@link GitHubPullRequestComment#getPullRequest()}
     * @param createdBy
     *            {@link GitHubPullRequestComment#getCreatedBy()}
     */
    void map(GitHubPullRequestComment target, Comment comment, GitHubPullRequest pullRequest, GitHubUser createdBy);

}
