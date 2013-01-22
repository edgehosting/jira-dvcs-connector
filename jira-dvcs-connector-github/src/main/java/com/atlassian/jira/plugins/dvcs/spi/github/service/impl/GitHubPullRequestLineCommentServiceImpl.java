package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import org.eclipse.egit.github.core.CommitComment;

import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubPullRequestLineCommentDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestLineComment;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestLineCommentService;

/**
 * The {@link GitHubPullRequestLineCommentService} implementation.
 * 
 * @author stanislav-dvorscak@solumiss.eu
 * 
 */
public class GitHubPullRequestLineCommentServiceImpl implements GitHubPullRequestLineCommentService
{

    /**
     * @see #GitHubPullRequestLineCommentServiceImpl(GitHubPullRequestLineCommentDAO)
     */
    private final GitHubPullRequestLineCommentDAO gitHubPullRequestLineCommentDAO;

    /**
     * Constructor.
     * 
     * @param gitHubPullRequestLineCommentDAO
     *            Injected {@link GitHubPullRequestLineCommentDAO} dependency.
     */
    public GitHubPullRequestLineCommentServiceImpl(GitHubPullRequestLineCommentDAO gitHubPullRequestLineCommentDAO)
    {
        this.gitHubPullRequestLineCommentDAO = gitHubPullRequestLineCommentDAO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(GitHubPullRequestLineComment gitHubPullRequestLineComment)
    {
        gitHubPullRequestLineCommentDAO.save(gitHubPullRequestLineComment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(GitHubPullRequestLineComment gitHubPullRequestLineComment)
    {
        gitHubPullRequestLineCommentDAO.delete(gitHubPullRequestLineComment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPullRequestLineComment getById(int id)
    {
        return gitHubPullRequestLineCommentDAO.getById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPullRequestLineComment getByGitHubId(long gitHubId)
    {
        return gitHubPullRequestLineCommentDAO.getByGitHubId(gitHubId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void map(GitHubPullRequestLineComment target, CommitComment source, GitHubPullRequest pullRequest, GitHubCommit commit)
    {
        target.setGitHubId(source.getId());
        target.setCreatedAt(source.getCreatedAt());
        target.setPullRequest(pullRequest);
        target.setCommit(commit);
        target.setPath(source.getPath());
        target.setLine(source.getLine());
        target.setText(source.getBodyText());
    }

}
