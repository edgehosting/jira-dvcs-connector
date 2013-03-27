package com.atlassian.jira.plugins.dvcs.spi.github.service;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommitComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;

/**
 * Provides services related to the {@link GitHubCommitComment}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface GitHubCommitCommentService
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
     *            size of offset
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
     * Synchronizes comment activities for provided domain repository.
     * 
     * @param domainRepository
     *            over which repository
     * @param domain
     *            over which repository
     */
    void synchronize(Repository domainRepository, GitHubRepository domain);

}
