package com.atlassian.jira.plugins.dvcs.spi.github.dao;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestComment;

/**
 * Provides {@link GitHubPullRequestComment} related DAO services.
 * 
 * @author stanislav-dvorscak@solumiss.eu
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

}
