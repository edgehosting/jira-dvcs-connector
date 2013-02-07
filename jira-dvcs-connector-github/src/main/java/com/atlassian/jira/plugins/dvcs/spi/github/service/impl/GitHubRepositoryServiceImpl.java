package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import java.io.IOException;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.RepositoryService;

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
    private GitHubRepositoryDAO gitHubRepositoryDAO;

    /**
     * @see #GitHubRepositoryServiceImpl(GitHubRepositoryDAO, GithubClientProvider)
     */
    private GithubClientProvider githubClientProvider;

    /**
     * Constructor.
     * 
     * @param gitHubRepositoryDAO
     *            injected {@link GitHubRepositoryDAO} dependency
     * @param githubClientProvider
     *            injected {@link GithubClientProvider} dependency
     */
    public GitHubRepositoryServiceImpl(GitHubRepositoryDAO gitHubRepositoryDAO, GithubClientProvider githubClientProvider)
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
    public GitHubRepository fetch(Repository repository, long gitHubId)
    {
        GitHubRepository result = getByGitHubId(gitHubId);
        if (result != null)
        {
            return result;
        }

        result = new GitHubRepository();
        RepositoryService repositoryService = githubClientProvider.getRepositoryService(repository);

        org.eclipse.egit.github.core.Repository loaded;
        try
        {
            loaded = repositoryService.getRepository(RepositoryId.createFromUrl(repository.getRepositoryUrl()));
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        result.setGitHubId(loaded.getId());
        result.setName(loaded.getName());
        save(result);
        return result;

    }
}
