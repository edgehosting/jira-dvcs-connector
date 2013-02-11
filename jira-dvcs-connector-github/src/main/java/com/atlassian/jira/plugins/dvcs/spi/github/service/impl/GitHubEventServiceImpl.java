package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubEventDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubEvent;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventService;

/**
 * Implementation of the {@link GitHubEventService}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubEventServiceImpl implements GitHubEventService
{

    /**
     * @see #GitHubSynchronizationInfoServiceImpl(GitHubEventDAO)
     */
    private final GitHubEventDAO gitHubEventDAO;

    /**
     * Constructor.
     * 
     * @param gitHubEventDAO
     *            injected {@link GitHubEventDAO} dependency
     */
    public GitHubEventServiceImpl(GitHubEventDAO gitHubEventDAO)
    {
        this.gitHubEventDAO = gitHubEventDAO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(GitHubEvent gitHubEvent)
    {
        gitHubEventDAO.save(gitHubEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubEvent getByGitHubId(String gitHubId)
    {
        return gitHubEventDAO.getByGitHubId(gitHubId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubEvent getLast(GitHubRepository gitHubRepository)
    {
        return gitHubEventDAO.getLast(gitHubRepository);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubEvent getLastSavePoint(GitHubRepository gitHubRepository)
    {
        return gitHubEventDAO.getLastSavePoint(gitHubRepository);
    }

}
