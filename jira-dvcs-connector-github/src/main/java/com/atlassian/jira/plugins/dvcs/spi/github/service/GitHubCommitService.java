package com.atlassian.jira.plugins.dvcs.spi.github.service;

import java.util.List;

import org.eclipse.egit.github.core.Commit;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;

/**
 * Defines {@link GitHubCommit}'s related services.
 * 
 * @author stanislav-dvorscak@solumiss.eu
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
     * Re-maps egit commit into the {@link GitHubCommit}.
     * 
     * @param target
     * @param source
     */
    void map(GitHubCommit target, Commit source);

}
