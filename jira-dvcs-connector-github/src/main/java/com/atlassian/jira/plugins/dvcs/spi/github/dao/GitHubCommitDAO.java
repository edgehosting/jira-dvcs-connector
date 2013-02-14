package com.atlassian.jira.plugins.dvcs.spi.github.dao;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;

/**
 * Defines {@link GitHubCommit}'s related DAO services.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface GitHubCommitDAO
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

}
