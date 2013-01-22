package com.atlassian.jira.plugins.dvcs.spi.github.dao;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestLineComment;

/**
 * The {@link GitHubPullRequestLineComment} related DAO services.
 * 
 * @author stanislav-dvorscak@solumiss.eu
 * 
 */
public interface GitHubPullRequestLineCommentDAO
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

}
