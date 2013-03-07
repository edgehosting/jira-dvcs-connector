package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestCommentMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubPullRequestLineCommentDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestLineComment;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestLineCommentService;

/**
 * The {@link GitHubPullRequestLineCommentService} implementation.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubPullRequestLineCommentServiceImpl implements GitHubPullRequestLineCommentService
{

    /**
     * @see #GitHubPullRequestLineCommentServiceImpl(GitHubPullRequestLineCommentDAO, RepositoryActivityDao, ActiveObjects)
     */
    private final GitHubPullRequestLineCommentDAO gitHubPullRequestLineCommentDAO;

    /**
     * @see #GitHubPullRequestLineCommentServiceImpl(GitHubPullRequestLineCommentDAO, RepositoryActivityDao, ActiveObjects)
     */
    private final RepositoryActivityDao repositoryActivityDao;

    /**
     * @see #GitHubPullRequestLineCommentServiceImpl(GitHubPullRequestLineCommentDAO, RepositoryActivityDao, ActiveObjects)
     */
    private final ActiveObjects activeObjects;

    /**
     * Constructor.
     * 
     * @param gitHubPullRequestLineCommentDAO
     *            injected {@link GitHubPullRequestLineCommentDAO} dependency
     * @param repositoryActivityDao
     *            injected {@link RepositoryActivityDao} dependency
     * @param activeObjects
     *            injected {@link ActiveObjects} dependency
     */
    public GitHubPullRequestLineCommentServiceImpl(GitHubPullRequestLineCommentDAO gitHubPullRequestLineCommentDAO,
            RepositoryActivityDao repositoryActivityDao, ActiveObjects activeObjects)
    {
        this.gitHubPullRequestLineCommentDAO = gitHubPullRequestLineCommentDAO;
        this.repositoryActivityDao = repositoryActivityDao;
        this.activeObjects = activeObjects;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(GitHubPullRequestLineComment gitHubPullRequestLineComment)
    {
        gitHubPullRequestLineCommentDAO.save(gitHubPullRequestLineComment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(GitHubPullRequestLineComment gitHubPullRequestLineComment)
    {
        gitHubPullRequestLineCommentDAO.delete(gitHubPullRequestLineComment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPullRequestLineComment getById(int id)
    {
        return gitHubPullRequestLineCommentDAO.getById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPullRequestLineComment getByGitHubId(long gitHubId)
    {
        return gitHubPullRequestLineCommentDAO.getByGitHubId(gitHubId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GitHubPullRequestLineComment> getByPullRequest(GitHubPullRequest pullRequest)
    {
        return gitHubPullRequestLineCommentDAO.getByPullRequest(pullRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void synchronize(Repository domainRepository, GitHubPullRequest pullRequest)
    {
        RepositoryPullRequestMapping repositoryPullRequest = repositoryActivityDao.findRequestByRemoteId(domainRepository.getId(),
                pullRequest.getGitHubId());

        Map<Long, RepositoryActivityPullRequestCommentMapping> idToLoaded = new HashMap<Long, RepositoryActivityPullRequestCommentMapping>();
        for (RepositoryActivityPullRequestCommentMapping loaded : repositoryActivityDao.getPullRequestComments(repositoryPullRequest))
        {
            if (!StringUtils.isEmpty(loaded.getFile())) {
                idToLoaded.put(loaded.getRemoteId(), loaded);
            }
        }

        Map<String, Object> activity = new HashMap<String, Object>();
        for (GitHubPullRequestLineComment comment : getByPullRequest(pullRequest))
        {
            if (!idToLoaded.containsKey(comment.getGitHubId()))
            {
                map(activity, repositoryPullRequest, comment);
                repositoryActivityDao.saveActivity(activity);
                activity.clear();
            } else
            {
                idToLoaded.remove(comment.getGitHubId());
            }
        }

        // removes comments which are not already propagated
        for (RepositoryActivityPullRequestCommentMapping toDelete : idToLoaded.values())
        {
            activeObjects.delete(toDelete);
        }
    }

    /**
     * Re-maps provided comment into the appropriate comment activity.
     * 
     * @param target
     *            activity
     * @param pullRequest
     *            owner of comment
     * @param source
     *            comment
     */
    private void map(Map<String, Object> target, RepositoryPullRequestMapping pullRequest, GitHubPullRequestLineComment source)
    {
        target.put(RepositoryActivityPullRequestMapping.PULL_REQUEST_ID, pullRequest.getID());
        target.put(RepositoryActivityPullRequestMapping.REPOSITORY_ID, pullRequest.getToRepositoryId());

        target.put(RepositoryActivityPullRequestCommentMapping.ENTITY_TYPE, RepositoryActivityPullRequestCommentMapping.class);
        target.put(RepositoryActivityPullRequestCommentMapping.REMOTE_ID, source.getGitHubId());
        target.put(RepositoryActivityPullRequestCommentMapping.LAST_UPDATED_ON, source.getCreatedAt());
        target.put(RepositoryActivityPullRequestCommentMapping.AUTHOR, source.getCreatedBy().getLogin());
        target.put(RepositoryActivityPullRequestCommentMapping.RAW_AUTHOR, source.getCreatedBy().getName());
        target.put(RepositoryActivityPullRequestCommentMapping.COMMENT_URL, source.getHtmlUrl());
        target.put(RepositoryActivityPullRequestCommentMapping.MESSAGE, source.getText());
        target.put(RepositoryActivityPullRequestCommentMapping.FILE, source.getPath());
    }

}
