package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import java.util.List;

import org.eclipse.egit.github.core.Comment;

import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubPullRequestCommentDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubUser;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestCommentService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestService;

/**
 * The implementation of the {@link GitHubPullRequestService}.
 * 
 * @author Stanislav Dvorscak
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
    public List<GitHubPullRequestComment> getAll()
    {
        return gitHubPullRequestCommentDAO.getAll();
    }

    /**
     * {@inheritDoc}
     * @param createdBy 
     */
    @Override
    public void map(GitHubPullRequestComment target, Comment source, GitHubPullRequest pullRequest, GitHubUser createdBy)
    {
        target.setGitHubId(source.getId());
        target.setPullRequest(pullRequest);
        target.setCreatedAt(source.getCreatedAt());
        target.setCreatedBy(createdBy);
        target.setText(source.getBody());
    }

}
