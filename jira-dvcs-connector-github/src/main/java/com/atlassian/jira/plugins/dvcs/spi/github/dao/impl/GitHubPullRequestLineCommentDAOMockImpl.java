package com.atlassian.jira.plugins.dvcs.spi.github.dao.impl;

import java.util.HashMap;
import java.util.Map;

import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubPullRequestLineCommentDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestLineComment;

/**
 * The transient/mock implementation of the {@link GitHubPullRequestLineCommentDAO}.
 * 
 * @author stanislav-dvorscak@solumiss.eu
 * 
 */
public class GitHubPullRequestLineCommentDAOMockImpl implements GitHubPullRequestLineCommentDAO
{

    /**
     * Transient store of the {@link GitHubPullRequestLineComment}-s.
     */
    private final Map<Integer, GitHubPullRequestLineComment> transientStore = new HashMap<Integer, GitHubPullRequestLineComment>();

    /**
     * Mapping from {@link GitHubPullRequestLineComment#getGitHubId()} to the {@link GitHubPullRequestLineComment#getId()}.
     */
    private final Map<Long, Integer> transientStoreByGitHubId = new HashMap<Long, Integer>();

    /**
     * Constructor.
     */
    public GitHubPullRequestLineCommentDAOMockImpl()
    {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(GitHubPullRequestLineComment gitHubPullRequestLineComment)
    {
        if (gitHubPullRequestLineComment.getId() == 0)
        {
            gitHubPullRequestLineComment.setId(transientStore.size() + 1);
        }

        transientStore.put(gitHubPullRequestLineComment.getId(), gitHubPullRequestLineComment);
        transientStoreByGitHubId.put(gitHubPullRequestLineComment.getGitHubId(), gitHubPullRequestLineComment.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(GitHubPullRequestLineComment gitHubPullRequestLineComment)
    {
        transientStore.remove(gitHubPullRequestLineComment.getId());
        transientStoreByGitHubId.remove(gitHubPullRequestLineComment.getGitHubId());
        gitHubPullRequestLineComment.setId(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPullRequestLineComment getById(int id)
    {
        return transientStore.get(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPullRequestLineComment getByGitHubId(long gitHubId)
    {
        Integer id = transientStoreByGitHubId.get(gitHubId);
        return id != null ? getById(id) : null;
    }

}
