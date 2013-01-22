package com.atlassian.jira.plugins.dvcs.spi.github.dao.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubCommitDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;

/**
 * Mock - transient implementation of the {@link GitHubCommitDAO}.
 * 
 * @author stanislav-dvorscak@solumiss.eu
 * 
 */
public class GitHubCommitDAOMockImpl implements GitHubCommitDAO
{

    /**
     * A {@link GitHubCommit#getId()} to the {@link GitHubCommit}.
     */
    private final Map<Integer, GitHubCommit> transientStore = new HashMap<Integer, GitHubCommit>();

    /**
     * A {@link GitHubCommit#getSha()} to the {@link GitHubCommit}.
     */
    private final Map<String, Integer> transientStoreBySha = new HashMap<String, Integer>();

    /**
     * Constructor.
     */
    public GitHubCommitDAOMockImpl()
    {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(GitHubCommit gitHubCommit)
    {
        if (gitHubCommit.getId() == 0)
        {
            gitHubCommit.setId(transientStore.size() + 1);
        }

        transientStore.put(gitHubCommit.getId(), gitHubCommit);
        transientStoreBySha.put(gitHubCommit.getSha(), gitHubCommit.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(GitHubCommit gitHubCommit)
    {
        transientStore.remove(gitHubCommit.getId());
        transientStoreBySha.remove(gitHubCommit.getSha());
        gitHubCommit.setId(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubCommit getById(int id)
    {
        return transientStore.get(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubCommit getBySha(String sha)
    {
        Integer id = transientStoreBySha.get(sha);
        return id != null ? transientStore.get(id) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GitHubCommit> getByIssueKey(String issueKey)
    {
        return new LinkedList<GitHubCommit>(transientStore.values());
    }

}
