package com.atlassian.jira.plugins.dvcs.spi.github.service;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
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
     * @param issueKey
     *            linked issue key
     * @return resolved {@link GitHubCommit}
     */
    List<GitHubCommit> getByIssueKey(String issueKey);

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
    public GitHubCommit fetch(Repository domainRepository, GitHubRepository domain, GitHubRepository repository, String sha);

}
