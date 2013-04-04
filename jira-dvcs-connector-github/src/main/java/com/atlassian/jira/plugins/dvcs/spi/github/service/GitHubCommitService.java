package com.atlassian.jira.plugins.dvcs.spi.github.service;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestUpdateActivityToCommitMapping;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;

/**
 * Defines {@link GitHubCommit}'s related services.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface GitHubCommitService
{

    /**
     * Saves or updates provided {@link GitHubCommit}.
     * 
     * @param gitHubCommit
     *            to save/update
     */
    void save(GitHubCommit gitHubCommit);

    /**
     * Deletes provided {@link GitHubCommit}.
     * 
     * @param gitHubCommit
     *            to delete
     */
    void delete(GitHubCommit gitHubCommit);

    /**
     * @param id
     *            {@link GitHubCommit#getId()}
     * @return resolved {@link GitHubCommit}
     */
    GitHubCommit getById(int id);

    /**
     * @param domain
     *            for repository
     * @param repository
     *            {@link GitHubCommit#getRepository()}
     * @param sha
     *            {@link GitHubCommit#getSha()}
     * @return resolved {@link GitHubCommit}
     */
    GitHubCommit getBySha(GitHubRepository domain, GitHubRepository repository, String sha);

    /**
     * @param domain
     *            over which repository
     * @param first
     *            offset of result
     * @param count
     *            size of result
     * @return all {@link GitHubCommit}s of provided repository
     */
    List<GitHubCommit> getAll(GitHubRepository domain, int first, int count);

    /**
     * @param domain
     *            over which repository
     * @return Rows count projection of {@link #getAll(GitHubRepository, int, int)}.
     */
    int getAllCount(GitHubRepository domain);

    /**
     * @param domainRepository
     *            for repository
     * @param domain
     *            for repository
     * @param repository
     *            owner of the commit
     * @param sha
     *            of the commit
     * @return newly created or existing commit
     */
    GitHubCommit fetch(Repository domainRepository, GitHubRepository domain, GitHubRepository repository, String sha);

    /**
     * Synchronizes all commits of provided repository with {@link RepositoryPullRequestUpdateActivityToCommitMapping}.
     * 
     * @param domainRepository
     *            for repository
     * @param domain
     *            for repository
     */
    void synchronize(Repository domainRepository, GitHubRepository domain);

    /**
     * Synchronizes {@link GitHubPullRequest#getCommits()} with {@link RepositoryPullRequestUpdateActivityToCommitMapping} holder.
     * 
     * @param domainRepository
     *            for repository
     * @param domain
     *            for repository
     * @param pullRequest
     *            owner of commits
     * @param progress
     *            indicating that synchronization should be stopped
     * @return newly created or existing commits
     */
    void synchronize(Repository domainRepository, GitHubRepository domain, GitHubPullRequest pullRequest, Progress progress);

}
