package com.atlassian.jira.plugins.dvcs.spi.github.service;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommitLineComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;

/**
 * Provides {@link GitHubCommitLineComment} related services.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface GitHubCommitLineCommentService
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
     * Synchronizes comment activities for provided domain repository.
     * 
     * @param domainRepository
     *            over which repository
     * @param domain
     *            over which repository
     */
    void synchronize(Repository domainRepository, GitHubRepository domain);

}
