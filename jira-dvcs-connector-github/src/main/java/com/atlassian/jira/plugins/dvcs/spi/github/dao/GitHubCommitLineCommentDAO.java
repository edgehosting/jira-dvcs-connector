package com.atlassian.jira.plugins.dvcs.spi.github.dao;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommitLineComment;

/**
 * Provides {@link GitHubCommitLineComment} related DAO services.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface GitHubCommitLineCommentDAO
{

    /**
     * Saves or updates provided {@link GitHubCommitLineComment}.
     * 
     * @param gitHubCommitLineComment
     *            to save/update
     */
    void save(GitHubCommitLineComment gitHubCommitLineComment);

    /**
     * Deletes provided {@link GitHubCommitLineComment}.
     * 
     * @param gitHubCommitLineComment
     *            to delete
     */
    void delete(GitHubCommitLineComment gitHubCommitLineComment);

    /**
     * @param id
     *            {@link GitHubCommitLineComment#getId()}
     * @return resolved {@link GitHubCommitLineComment}
     */
    GitHubCommitLineComment getById(int id);

    /**
     * @param gitHubId
     *            {@link GitHubCommitLineComment#getGitHubId()}
     * @return resolved {@link GitHubCommitLineComment}
     */
    GitHubCommitLineComment getByGitHubId(long gitHubId);

}
