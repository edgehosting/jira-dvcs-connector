package com.atlassian.jira.plugins.dvcs.spi.github.dao.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubPullRequestCommentDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestComment;

/**
 * Transient mock implementation of the {@link GitHubPullRequestCommentDAO}.
 * 
 * @author stanislav-dvorscak@solumiss.eu
 * 
 */
public class GitHubPullRequestCommentDAOMockImpl implements GitHubPullRequestCommentDAO
{

    /**
     * Transient store.
     */
    private Map<Integer, GitHubPullRequestComment> transientStore = new ConcurrentHashMap<Integer, GitHubPullRequestComment>();

    /**
     * Transient store - {@link GitHubPullRequestComment#getGitHubId()} to the {@link GitHubPullRequestComment#getGitHubId()}.
     */
    private Map<Long, Integer> transientStoreByGitHubId = new ConcurrentHashMap<Long, Integer>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(GitHubPullRequestComment gitHubPullRequestComment)
    {
        if (gitHubPullRequestComment.getId() == 0)
        {
            gitHubPullRequestComment.setId(transientStore.size() + 1);
        }

        transientStore.put(gitHubPullRequestComment.getId(), gitHubPullRequestComment);
        transientStoreByGitHubId.put(gitHubPullRequestComment.getGitHubId(), gitHubPullRequestComment.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(GitHubPullRequestComment gitHubPullRequestComment)
    {
        transientStore.remove(gitHubPullRequestComment.getId());
        gitHubPullRequestComment.setId(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPullRequestComment getById(int id)
    {
        return transientStore.get(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPullRequestComment getByGitHubId(long gitHubId)
    {
        Integer id = transientStoreByGitHubId.get(gitHubId);
        return id != null ? getById(id) : null;
    }

}
