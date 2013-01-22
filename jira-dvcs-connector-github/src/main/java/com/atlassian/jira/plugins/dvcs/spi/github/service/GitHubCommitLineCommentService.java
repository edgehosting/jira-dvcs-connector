package com.atlassian.jira.plugins.dvcs.spi.github.service;

import org.eclipse.egit.github.core.CommitComment;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommitLineComment;

/**
 * Provides {@link GitHubCommitLineComment} related services.
 * 
 * @author stanislav-dvorscak@solumiss.eu
 * 
 */
public interface GitHubCommitLineCommentService
{

    /**
     * Saves or updates provided {@link GitHubCommitLineComment}.
     * 
     * @param gitHubCommitLineComment
     *            to save/update
     */
    void save(GitHubCommitLineComment gitHubCommitLineComment);

    /**
     * Deletes provided {@link GitHubCommitLineComment}.
     * 
     * @param gitHubCommitLineComment
     *            to delete
     */
    void delete(GitHubCommitLineComment gitHubCommitLineComment);

    /**
     * @param id
     *            {@link GitHubCommitLineComment#getId()}
     * @return resolved {@link GitHubCommitLineComment}
     */
    GitHubCommitLineComment getById(int id);

    /**
     * @param gitHubId
     *            {@link GitHubCommitLineComment#getGitHubId()}
     * @return resolved {@link GitHubCommitLineComment}
     */
    GitHubCommitLineComment getByGitHubId(long gitHubId);

    /**
     * Re-maps egit model into the internal model.
     * 
     * @param target
     *            internal model
     * @param source
     *            egit model
     * @param commit
     *            re-mapped {@link CommitComment#getCommitId()}
     */
    public void map(GitHubCommitLineComment target, CommitComment source, GitHubCommit commit);

}
