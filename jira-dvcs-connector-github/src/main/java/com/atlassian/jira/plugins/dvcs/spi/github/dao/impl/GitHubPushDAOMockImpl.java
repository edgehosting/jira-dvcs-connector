package com.atlassian.jira.plugins.dvcs.spi.github.dao.impl;

import java.util.HashMap;
import java.util.Map;

import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubPushDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPush;

/**
 * Mock - transient implementation of the {@link GitHubPushDAO}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubPushDAOMockImpl implements GitHubPushDAO
{

    /**
     * A {@link GitHubPush#getId()} to the {@link GitHubPush}.
     */
    private Map<Integer, GitHubPush> transientStore = new HashMap<Integer, GitHubPush>();

    /**
     * A {@link GitHubPush#getBefore()} to the {@link GitHubPush}.
     */
    private Map<String, Integer> transientStoreByBefore = new HashMap<String, Integer>();

    /**
     * A {@link GitHubPush#getHead()} to the {@link GitHubPush}.
     */
    private Map<String, Integer> transientStoreByHead = new HashMap<String, Integer>();

    /**
     * Constructor.
     */
    public GitHubPushDAOMockImpl()
    {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(GitHubPush gitHubPush)
    {
        if (gitHubPush.getId() == 0)
        {
            gitHubPush.setId(transientStore.size() + 1);
        }

        transientStore.put(gitHubPush.getId(), gitHubPush);
        transientStoreByBefore.put(gitHubPush.getBefore(), gitHubPush.getId());
        transientStoreByHead.put(gitHubPush.getHead(), gitHubPush.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(GitHubPush gitHubPush)
    {
        transientStore.remove(gitHubPush.getId());
        transientStoreByBefore.remove(gitHubPush.getBefore());
        transientStoreByHead.remove(gitHubPush.getHead());
        gitHubPush.setId(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPush getById(int id)
    {
        return transientStore.get(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPush getByBefore(String sha)
    {
        Integer id = transientStoreByBefore.get(sha);
        return id != null ? getById(id) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPush getByHead(String sha)
    {
        Integer id = transientStoreByHead.get(sha);
        return id != null ? getById(id) : null;
    }

}
