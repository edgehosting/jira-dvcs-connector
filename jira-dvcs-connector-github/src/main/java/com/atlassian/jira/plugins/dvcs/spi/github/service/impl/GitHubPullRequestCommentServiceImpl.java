package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import org.eclipse.egit.github.core.CommitComment;

import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubPullRequestCommentDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestComment;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestCommentService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestService;

/**
 * The implementation of the {@link GitHubPullRequestService}.
 * 
 * @author stanislav-dvorscak@solumiss.eu
 * 
 */
public class GitHubPullRequestCommentServiceImpl implements GitHubPullRequestCommentService
{

    /**
     * @see #GitHubPullRequestCommentServiceImpl(GitHubPullRequestCommentDAO)
     */
    private final GitHubPullRequestCommentDAO gitHubPullRequestCommentDAO;

    /**
     * Constructor.
     * 
     * @param gitHubPullRequestCommentDAO
     *            Injected {@link GitHubPullRequestCommentDAO} dependency.
     */
    public GitHubPullRequestCommentServiceImpl(GitHubPullRequestCommentDAO gitHubPullRequestCommentDAO)
    {
        this.gitHubPullRequestCommentDAO = gitHubPullRequestCommentDAO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(GitHubPullRequestComment gitHubPullRequestComment)
    {
        gitHubPullRequestCommentDAO.save(gitHubPullRequestComment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(GitHubPullRequestComment gitHubPullRequestComment)
    {
        gitHubPullRequestCommentDAO.delete(gitHubPullRequestComment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPullRequestComment getById(int id)
    {
        return gitHubPullRequestCommentDAO.getById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPullRequestComment getByGitHubId(long gitHubId)
    {
        return gitHubPullRequestCommentDAO.getByGitHubId(gitHubId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void map(GitHubPullRequestComment target, CommitComment source, GitHubPullRequest pullRequest)
    {
        target.setGitHubId(source.getId());
        target.setText(source.getBodyText());
        target.setPullRequest(pullRequest);
    }

}
