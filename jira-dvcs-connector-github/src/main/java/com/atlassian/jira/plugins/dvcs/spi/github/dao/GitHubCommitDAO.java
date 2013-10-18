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

}
