package com.atlassian.jira.plugins.dvcs.spi.github.dao.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubCommitCommentDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommitComment;

/**
 * Transient mock implementation of the {@link GitHubCommitComment}.
 * 
 * @author stanislav-dvorscak@solumiss.eu
 * 
 */
public class GitHubCommitCommentDAOMockImpl implements GitHubCommitCommentDAO
{

    /**
     * Transient store between {@link GitHubCommitComment#getId()} and appropriate {@link GitHubCommitComment}.
     */
    private Map<Integer, GitHubCommitComment> transientStore = new HashMap<Integer, GitHubCommitComment>();

    /**
     * Maps between {@link GitHubCommitComment#getGitHubId()} and {@link GitHubCommitComment#getId()}.
     */
    private Map<Long, Integer> gitHubIdToId = new ConcurrentHashMap<Long, Integer>();

    /**
     * Constructor.
     */
    public GitHubCommitCommentDAOMockImpl()
    {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(GitHubCommitComment gitHubCommitComment)
    {
        if (gitHubCommitComment.getId() == 0)
        {
            gitHubCommitComment.setId(transientStore.size() + 1);
        }

        transientStore.put(gitHubCommitComment.getId(), gitHubCommitComment);
        gitHubIdToId.put(gitHubCommitComment.getGitHubId(), gitHubCommitComment.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(GitHubCommitComment gitHubCommitComment)
    {
        transientStore.remove(gitHubCommitComment.getId());
        gitHubIdToId.remove(gitHubCommitComment.getGitHubId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubCommitComment getById(int id)
    {
        return transientStore.get(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubCommitComment getByGitHubId(long gitHubId)
    {
        Integer id = gitHubIdToId.get(gitHubId);
        return id != null ? getById(id) : null;
    }

}
