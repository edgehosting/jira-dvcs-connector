package com.atlassian.jira.plugins.dvcs.spi.github.dao.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubUserDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubUser;

/**
 * Mock/transient implementation of the {@link GitHubUserDAO}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubUserDAOMockImpl implements GitHubUserDAO
{

    /**
     * Mock - transient store for stored GitGubUsers.
     */
    private Map<Integer, GitHubUser> transientStore = new ConcurrentHashMap<Integer, GitHubUser>();

    /**
     * Maps {@link GitHubUser#getLogin()} to the appropriate {@link GitHubUser#getId()}.
     */
    private Map<String, Integer> loginToId = new ConcurrentHashMap<String, Integer>();

    /**
     * Constructor.
     */
    public GitHubUserDAOMockImpl()
    {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(GitHubUser gitHubUser)
    {
        if (gitHubUser.getId() == 0)
        {
            gitHubUser.setId(transientStore.size() + 1);
        }

        transientStore.put(gitHubUser.getId(), gitHubUser);
        loginToId.put(gitHubUser.getLogin(), gitHubUser.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(GitHubUser gitHubUser)
    {
        transientStore.remove(gitHubUser.getId());
        loginToId.remove(gitHubUser.getLogin());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubUser getById(int id)
    {
        return transientStore.get(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubUser getByLogin(String login)
    {
        Integer id = loginToId.get(login);
        return id != null ? getById(id) : null;
    }

}
