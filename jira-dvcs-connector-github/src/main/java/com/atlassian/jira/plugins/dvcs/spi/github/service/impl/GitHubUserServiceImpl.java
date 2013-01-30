package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import java.io.IOException;
import java.util.Date;

import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.UserService;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubUserDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubUser;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubUserService;

/**
 * The {@link GitHubUserService} implementation.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubUserServiceImpl implements GitHubUserService
{

    /**
     * @see #GitHubUserServiceImpl(GitHubUserDAO, GithubClientProvider)
     */
    private final GitHubUserDAO gitHubUserDAO;

    /**
     * @see #GitHubUserServiceImpl(GitHubUserDAO, GithubClientProvider)
     */
    private final GithubClientProvider githubClientProvider;

    /**
     * Constructor.
     * 
     * @param gitHubUserDAO
     *            Injected {@link GitHubUserDAO} dependency.
     * @param githubClientProvider
     *            Injected {@link GithubClientProvider} dependency.
     */
    public GitHubUserServiceImpl(GitHubUserDAO gitHubUserDAO, GithubClientProvider githubClientProvider)
    {
        this.gitHubUserDAO = gitHubUserDAO;
        this.githubClientProvider = githubClientProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(GitHubUser gitHubUser)
    {
        gitHubUserDAO.save(gitHubUser);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(GitHubUser gitHubUser)
    {
        gitHubUserDAO.delete(gitHubUser);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubUser getByLogin(String login)
    {
        return gitHubUserDAO.getByLogin(login);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubUser synchronize(String login, Repository repository)
    {
        GitHubUser result = getByLogin(login);
        if (result == null)
        {
            result = new GitHubUser();

        }

        UserService userService = githubClientProvider.getUserService(repository);
        User loaded;
        try
        {
            loaded = userService.getUser(login);

        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        result.setSynchronizedAt(new Date());
        result.setGitHubId(loaded.getId());
        result.setLogin(loaded.getLogin());
        result.setName(loaded.getName());
        result.setEmail(loaded.getEmail());
        result.setAvatarUrl(loaded.getAvatarUrl());
        result.setUrl(loaded.getHtmlUrl());
        save(result);

        return result;
    }

}
