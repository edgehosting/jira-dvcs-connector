package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubPushDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPush;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPushService;

/**
 * The {@link GitHubPushService} implementation.
 * 
 * @author stanislav-dvorscak@solumiss.eu
 * 
 */
public class GitHubPushServiceImpl implements GitHubPushService
{

    /**
     * @see #GitHubPushServiceImpl(GitHubPushDAO)
     */
    private final GitHubPushDAO gitHubPushDAO;

    /**
     * Injected {@link GitHubPushDAO} dependency.
     * 
     * @param gitHubPushDAO
     */
    public GitHubPushServiceImpl(GitHubPushDAO gitHubPushDAO)
    {
        this.gitHubPushDAO = gitHubPushDAO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(GitHubPush gitHubPush)
    {
        gitHubPushDAO.save(gitHubPush);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(GitHubPush gitHubPush)
    {
        gitHubPushDAO.delete(gitHubPush);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPush getById(int id)
    {
        return gitHubPushDAO.getById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPush getByBefore(String sha)
    {
        return gitHubPushDAO.getByBefore(sha);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPush getByHead(String sha)
    {
        return gitHubPushDAO.getByHead(sha);
    }

}
