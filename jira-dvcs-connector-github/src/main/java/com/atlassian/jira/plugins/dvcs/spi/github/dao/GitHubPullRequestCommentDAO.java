package com.atlassian.jira.plugins.dvcs.spi.github.dao;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestComment;

/**
 * Provides {@link GitHubPullRequestComment} related DAO services.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface GitHubPullRequestCommentDAO
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

}
