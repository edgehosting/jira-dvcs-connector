package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import java.io.IOException;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubRepositoryDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubRepositoryService;

/**
 * Implementation of the {@link GitHubRepositoryService}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubRepositoryServiceImpl implements GitHubRepositoryService
{

    /**
     * @see #GitHubRepositoryServiceImpl(GitHubRepositoryDAO, GithubClientProvider)
     */
    private final GitHubRepositoryDAO gitHubRepositoryDAO;

    /**
     * @see #GitHubRepositoryServiceImpl(GitHubRepositoryDAO, GithubClientProvider)
     */
    private final GithubClientProvider githubClientProvider;

    /**
     * Constructor.
     * 
     * @param gitHubRepositoryDAO
     *            injected {@link GitHubRepositoryDAO} dependency
     * @param githubClientProvider
     *            injected {@link GithubClientProvider} dependency
     */
    public GitHubRepositoryServiceImpl(GitHubRepositoryDAO gitHubRepositoryDAO, @Qualifier("githubClientProvider") GithubClientProvider githubClientProvider)
    {
        this.gitHubRepositoryDAO = gitHubRepositoryDAO;
        this.githubClientProvider = githubClientProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(GitHubRepository gitHubRepository)
    {
        gitHubRepositoryDAO.save(gitHubRepository);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(GitHubRepository gitHubRepository)
    {
        gitHubRepositoryDAO.delete(gitHubRepository);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubRepository getByGitHubId(long gitHubId)
    {
        return gitHubRepositoryDAO.getByGitHubId(gitHubId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubRepository fetch(Repository domainRepository, String owner, String name, long gitHubId)
    {
        // loaded by GitHubId
        GitHubRepository result = gitHubId != 0 ? getByGitHubId(gitHubId) : null;
        if (result != null)
        {
            return result;
        }

        // loaded via REST
        RepositoryService egitRepositoryService = githubClientProvider.getRepositoryService(domainRepository);
        org.eclipse.egit.github.core.Repository loaded;
        try
        {
            loaded = egitRepositoryService.getRepository(RepositoryId.createFromId(owner + "/" + name));
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        // reuse existing
        result = gitHubId == 0 ? getByGitHubId(loaded.getId()) : null;
        if (result == null)
        {
            result = new GitHubRepository();
        }

        // re-maps fetched values
        result.setGitHubId(loaded.getId());
        result.setName(loaded.getName());
        result.setUrl(loaded.getHtmlUrl());
        save(result);

        return result;

    }
}
