package com.atlassian.jira.plugins.dvcs.spi.github.dao.impl;

import java.util.HashMap;
import java.util.Map;

import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubCommitLineCommentDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommitLineComment;

/**
 * Mock transient implementation of the {@link GitHubCommitLineComment}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubCommitLineCommentDAOMockImpl implements GitHubCommitLineCommentDAO
{

    /**
     * Transient store - {@link GitHubCommitLineComment#getId()} to the {@link GitHubCommitLineComment}.
     */
    private Map<Integer, GitHubCommitLineComment> transientStore = new HashMap<Integer, GitHubCommitLineComment>();

    /**
     * Transient store - {@link GitHubCommitLineComment#getGitHubId()} to the {@link GitHubCommitLineComment#getId()}.
     */
    private Map<Long, Integer> transientStoreByGitHubId = new HashMap<Long, Integer>();

    /**
     * Constructor.
     */
    public GitHubCommitLineCommentDAOMockImpl()
    {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(GitHubCommitLineComment gitHubCommitLineComment)
    {
        if (gitHubCommitLineComment.getId() == 0)
        {
            gitHubCommitLineComment.setId(transientStore.size() + 1);
        }

        transientStore.put(gitHubCommitLineComment.getId(), gitHubCommitLineComment);
        transientStoreByGitHubId.put(gitHubCommitLineComment.getGitHubId(), gitHubCommitLineComment.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(GitHubCommitLineComment gitHubCommitLineComment)
    {
        transientStore.remove(gitHubCommitLineComment.getGitHubId());
        transientStoreByGitHubId.remove(gitHubCommitLineComment.getGitHubId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubCommitLineComment getById(int id)
    {
        return transientStore.get(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubCommitLineComment getByGitHubId(long gitHubId)
    {
        Integer id = transientStoreByGitHubId.get(gitHubId);
        return id != null ? getById(id) : null;
    }

}
