package com.atlassian.jira.plugins.dvcs.spi.github.dao;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommitComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;

/**
 * Provides DAO services related to the {@link GitHubCommitComment}.
 * 
 * @author Stanislav Dvorscak
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

    /**
     * Returns all comments.
     * 
     * @param domain
     *            over which repository
     * @param first
     *            result offset
     * @param count
     *            size of result
     * @return subset of all comments
     */
    List<GitHubCommitComment> getAll(GitHubRepository domain, int first, int count);

    /**
     * @param domain
     *            over which repository
     * @return Rows count projection of {@link #getAll(int, int)}.
     */
    int getAllCount(GitHubRepository domain);

    /**
     * @param domain
     * 
     * @return Returns all commented commits.
     */
    List<GitHubCommit> getCommentedCommits(GitHubRepository domain, int first, int count);

    /**
     * @param domain
     *            over which repository
     * @param commit
     *            for which comment
     * @return commit comment
     */
    List<GitHubCommitComment> getByCommit(GitHubRepository domain, GitHubCommit commit);

}
