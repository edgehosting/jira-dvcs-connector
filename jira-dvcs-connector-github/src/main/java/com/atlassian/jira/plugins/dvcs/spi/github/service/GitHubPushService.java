package com.atlassian.jira.plugins.dvcs.spi.github.service;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPush;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;

/**
 * Provides services related to the {@link GitHubPush}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface GitHubPushService
{

    /**
     * @param gitHubPush
     *            to save/update
     */
    void save(GitHubPush gitHubPush);

    /**
     * @param gitHubPush
     *            to delete
     */
    void delete(GitHubPush gitHubPush);

    /**
     * @param id
     *            {@link GitHubPush#getId()}
     * @return resolved {@link GitHubPush}
     */
    GitHubPush getById(int id);

    /**
     * @param repository
     *            for which repository
     * @param sha
     *            {@link GitHubPush#getBefore()}
     * @return resolved {@link GitHubPush}
     */
    GitHubPush getByBefore(GitHubRepository repository, String sha);

    /**
     * @param repository
     *            for which repository
     * @param sha
     *            {@link GitHubPush#getHead()}
     * @return resolved {@link GitHubPush}
     */
    GitHubPush getByHead(GitHubRepository repository, String sha);

    /**
     * Returns all pushes done on the repository between the provided commit SHA-s.
     * 
     * @param repository
     *            over which repository
     * @param fromSha
     *            start point (include)
     * @param toSha
     *            end point (include)
     * @return all pushes done from provided sha
     */
    List<GitHubPush> getByBetween(GitHubRepository repository, String fromSha, String toSha);

}
