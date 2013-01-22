package com.atlassian.jira.plugins.dvcs.spi.github.dao;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommitComment;

/**
 * Provides DAO services related to the {@link GitHubCommitComment}.
 * 
 * @author stanislav-dvorscak@solumiss.eu
 * 
 */
public interface GitHubCommitCommentDAO
{

    /**
     * Saves or updates provided {@link GitHubCommitComment}.
     * 
     * @param gitHubCommitComment
     *            to save/update
     */
    void save(GitHubCommitComment gitHubCommitComment);

    /**
     * Deletes provided {@link GitHubCommitComment}.
     * 
     * @param gitHubCommitComment
     *            to delete
     */
    void delete(GitHubCommitComment gitHubCommitComment);

    /**
     * @param id
     *            {@link GitHubCommitComment#getId()}
     * @return resolved {@link GitHubCommitComment}.
     */
    GitHubCommitComment getById(int id);

    /**
     * @param gitHubId
     *            {@link GitHubCommitComment#getGitHubId()}
     * @return resolved {@link GitHubCommitComment}
     */
    GitHubCommitComment getByGitHubId(long gitHubId);

}
