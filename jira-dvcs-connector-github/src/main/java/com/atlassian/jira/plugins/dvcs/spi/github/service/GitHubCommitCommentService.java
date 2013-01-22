package com.atlassian.jira.plugins.dvcs.spi.github.service;

import org.eclipse.egit.github.core.CommitComment;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommitComment;

/**
 * Provides services related to the {@link GitHubCommitComment}.
 * 
 * @author stanislav-dvorscak@solumiss.eu
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
     * Re-maps egit model into the internal model.
     * 
     * @param target
     *            internal model
     * @param source
     *            egit model
     * @param gitHubCommit
     *            already re-mapped {@link CommitComment#getCommitId()}
     */
    public void map(GitHubCommitComment target, CommitComment source, GitHubCommit gitHubCommit);

}
