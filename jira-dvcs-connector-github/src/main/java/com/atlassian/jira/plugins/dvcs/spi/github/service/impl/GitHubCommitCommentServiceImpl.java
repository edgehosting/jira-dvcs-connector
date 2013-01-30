package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import org.eclipse.egit.github.core.CommitComment;

import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubCommitCommentDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommitComment;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubCommitCommentService;

/**
 * The implementation of the {@link GitHubCommitCommentService}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubCommitCommentServiceImpl implements GitHubCommitCommentService
{

    /**
     * @see #GitHubCommitCommentServiceImpl(GitHubCommitCommentDAO)
     */
    private final GitHubCommitCommentDAO gitHubCommitCommentDAO;

    /**
     * Constructor.
     * 
     * @param gitHubCommitCommentDAO
     *            Injected {@link GitHubCommitComment} dependency.
     */
    public GitHubCommitCommentServiceImpl(GitHubCommitCommentDAO gitHubCommitCommentDAO)
    {
        this.gitHubCommitCommentDAO = gitHubCommitCommentDAO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(GitHubCommitComment gitHubCommitComment)
    {
        gitHubCommitCommentDAO.save(gitHubCommitComment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(GitHubCommitComment gitHubCommitComment)
    {
        gitHubCommitCommentDAO.delete(gitHubCommitComment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubCommitComment getById(int id)
    {
        return gitHubCommitCommentDAO.getById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubCommitComment getByGitHubId(long gitHubId)
    {
        return gitHubCommitCommentDAO.getByGitHubId(gitHubId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void map(GitHubCommitComment target, CommitComment source, GitHubCommit gitHubCommit)
    {
        target.setGitHubId(source.getId());
        target.setCreatedAt(source.getCreatedAt());
        target.setText(source.getBodyText());
        target.setCommit(gitHubCommit);
    }

}
