package com.atlassian.jira.plugins.dvcs.spi.github.dao;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommitLineComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;

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

    /**
     * Returns all in-line comments.
     * 
     * @param domain
     *            over which repository
     * @param first
     *            result offset
     * @param count
     *            size of result
     * @return subset of all comments
     */
    List<GitHubCommitLineComment> getAll(GitHubRepository domain, int first, int count);

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
    List<GitHubCommitLineComment> getByCommit(GitHubRepository domain, GitHubCommit commit);

}
