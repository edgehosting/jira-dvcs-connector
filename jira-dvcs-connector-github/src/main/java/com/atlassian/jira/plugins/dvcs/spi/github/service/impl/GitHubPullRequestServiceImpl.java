package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import java.util.List;

import org.eclipse.egit.github.core.PullRequest;

import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubPullRequestDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestService;

/**
 * An {@link GitHubPullRequestService} implementation.
 * 
 * @author stanislav-dvorscak@solumiss.eu
 * 
 */
public class GitHubPullRequestServiceImpl implements GitHubPullRequestService
{

    /**
     * @see #GitHubPullRequestServiceImpl(GitHubPullRequestDAO)
     */
    private final GitHubPullRequestDAO gitHubPullRequestDAO;

    /**
     * Constructor.
     * 
     * @param gitHubPullRequestDAO
     *            injected {@link GitHubPullRequestDAO} dependency.
     */
    public GitHubPullRequestServiceImpl(GitHubPullRequestDAO gitHubPullRequestDAO)
    {
        this.gitHubPullRequestDAO = gitHubPullRequestDAO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(GitHubPullRequest gitHubPullRequest)
    {
        gitHubPullRequestDAO.save(gitHubPullRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(GitHubPullRequest gitHubPullRequest)
    {
        gitHubPullRequestDAO.delete(gitHubPullRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPullRequest getById(int id)
    {
        return gitHubPullRequestDAO.getById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPullRequest getByGitHubId(long gitHubId)
    {
        return gitHubPullRequestDAO.getByGitHubId(gitHubId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GitHubPullRequest> getGitHubPullRequest(String issueKey)
    {
        return gitHubPullRequestDAO.getGitHubPullRequest(issueKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void map(GitHubPullRequest target, PullRequest source)
    {
        target.setGitHubId(source.getId());
        target.setTitle(source.getTitle());
        target.setCreatedAt(source.getCreatedAt());
    }

}
