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
     * @param sha
     *            {@link GitHubCommit#getSha()}
     * @return resolved {@link GitHubCommit}
     */
    GitHubCommit getBySha(String sha);

    /**
     * @param issueKey
     *            linked issue key
     * @return resolved {@link GitHubCommit}
     */
    List<GitHubCommit> getByIssueKey(String issueKey);

    /**
     * @param gitHubRepository
     *            over which repository it is done
     * @param repository
     *            where should be commit
     * @param sha
     *            of the commit
     * @return newly created or existing commit
     */
    public GitHubCommit fetch(GitHubRepository gitHubRepository, Repository repository, String sha);

}
