package com.atlassian.jira.plugins.dvcs.spi.github.service;

import java.util.List;

import org.eclipse.egit.github.core.CommitComment;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestLineComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubUser;

/**
 * The {@link GitHubPullRequestLineComment} related services.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface GitHubPullRequestLineCommentService
{

    /**
     * Saves or updates provided {@link GitHubPullRequestLineComment}.
     * 
     * @param gitHubPullRequestLineComment
     *            to save/update
     */
    void save(GitHubPullRequestLineComment gitHubPullRequestLineComment);

    /**
     * Deletes provided {@link GitHubPullRequestLineComment}.
     * 
     * @param gitHubPullRequestLineComment
     *            to delete
     */
    void delete(GitHubPullRequestLineComment gitHubPullRequestLineComment);

    /**
     * @param id
     *            {@link GitHubPullRequestLineComment#getId()}
     * @return resolved {@link GitHubPullRequestLineComment}
     */
    GitHubPullRequestLineComment getById(int id);

    /**
     * @param gitHubId
     *            {@link GitHubPullRequestLineComment#getGitHubId()}
     * @return {@link GitHubPullRequestLineComment}
     */
    GitHubPullRequestLineComment getByGitHubId(long gitHubId);

    /**
     * Re-maps egit model to the internal model.
     * 
     * @param target
     *            internal model
     * @param source
     *            egit model
     * @param pullRequest
     *            {@link GitHubPullRequestLineComment#getPullRequest()}
     * @param createdBy
     *            {@link GitHubPullRequestLineComment#getCreatedBy()}
     * @param commit
     *            {@link CommitComment#getCommitId()}
     */
    public void map(GitHubPullRequestLineComment target, CommitComment source, GitHubPullRequest pullRequest, GitHubUser createdBy,
            GitHubCommit commit);

    /**
     * @param repository
     *            {@link GitHubPullRequestLineComment#getDomain()}
     * @return resolved {@link GitHubPullRequestLineComment}-s
     */
    List<GitHubPullRequestLineComment> getByRepository(GitHubRepository repository);

}
