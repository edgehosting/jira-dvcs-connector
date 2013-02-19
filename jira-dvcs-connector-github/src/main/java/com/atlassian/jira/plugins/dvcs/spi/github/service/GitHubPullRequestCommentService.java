package com.atlassian.jira.plugins.dvcs.spi.github.service;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;

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
     * @param repository
     *            {@link GitHubPullRequestComment#getDomain()}
     * @return resolved {@link GitHubPullRequestComment}-s
     */
    List<GitHubPullRequestComment> getByRepository(GitHubRepository repository);

    /**
     * Synchronizes all comments and appropriate repository activities.
     * 
     * @param domainRepository
     *            for repository
     * @param domain
     *            for repository
     */
    void synchronize(Repository domainRepository, GitHubRepository domain);
}
