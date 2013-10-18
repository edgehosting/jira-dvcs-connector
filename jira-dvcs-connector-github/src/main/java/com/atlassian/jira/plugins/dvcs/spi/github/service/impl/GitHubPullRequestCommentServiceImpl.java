package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import java.util.List;
import java.util.Map;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubPullRequestCommentDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestComment;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestCommentService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestService;

/**
 * The implementation of the {@link GitHubPullRequestService}.
 *
 * @author Stanislav Dvorscak
 *
 */
public class GitHubPullRequestCommentServiceImpl implements GitHubPullRequestCommentService
{

    /**
     * @see #GitHubPullRequestCommentServiceImpl(GitHubPullRequestCommentDAO, RepositoryActivityDao)
     */
    private final GitHubPullRequestCommentDAO gitHubPullRequestCommentDAO;

    /**
     * @see #GitHubPullRequestCommentServiceImpl(GitHubPullRequestCommentDAO, RepositoryActivityDao)
     */
    private final RepositoryActivityDao repositoryActivityDao;

    /**
     * @see #GitHubPullRequestCommentServiceImpl(GitHubPullRequestCommentDAO, RepositoryActivityDao)
     */
    private final ActiveObjects activeObjects;

    /**
     * Constructor.
     *
     * @param gitHubPullRequestCommentDAO
     *            injected {@link GitHubPullRequestCommentDAO} dependency
     * @param repositoryActivityDao
     *            injected {@link RepositoryActivityDao} dependency
     * @param activeObjects
     *            injected {@link ActiveObjects} dependency
     */
    public GitHubPullRequestCommentServiceImpl(GitHubPullRequestCommentDAO gitHubPullRequestCommentDAO,
            RepositoryActivityDao repositoryActivityDao, ActiveObjects activeObjects)
    {
        this.gitHubPullRequestCommentDAO = gitHubPullRequestCommentDAO;
        this.repositoryActivityDao = repositoryActivityDao;
        this.activeObjects = activeObjects;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(GitHubPullRequestComment gitHubPullRequestComment)
    {
        gitHubPullRequestCommentDAO.save(gitHubPullRequestComment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(GitHubPullRequestComment gitHubPullRequestComment)
    {
        gitHubPullRequestCommentDAO.delete(gitHubPullRequestComment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPullRequestComment getById(int id)
    {
        return gitHubPullRequestCommentDAO.getById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPullRequestComment getByGitHubId(long gitHubId)
    {
        return gitHubPullRequestCommentDAO.getByGitHubId(gitHubId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GitHubPullRequestComment> getByPullRequest(GitHubPullRequest pullRequest)
    {
        return gitHubPullRequestCommentDAO.getByPullRequest(pullRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void synchronize(Repository domainRepository, GitHubPullRequest pullRequest, Progress progress)
    {
        if (progress.isShouldStop())
        {
            return;
        }

        RepositoryPullRequestMapping repositoryPullRequest = repositoryActivityDao.findRequestByRemoteId(domainRepository,
                pullRequest.getGitHubId());

//        Map<Long, RepositoryPullRequestCommentActivityMapping> idToLoaded = new HashMap<Long, RepositoryPullRequestCommentActivityMapping>();
//        for (RepositoryPullRequestCommentActivityMapping loaded : repositoryActivityDao.getPullRequestComments(domainRepository,
//                repositoryPullRequest))
//        {
//            idToLoaded.put(loaded.getRemoteId(), loaded);
//        }
//
//        for (GitHubPullRequestComment comment : getByPullRequest(pullRequest))
//        {
//            if (progress.isShouldStop())
//            {
//                return;
//            }
//
//            Map<String, Object> activity = new HashMap<String, Object>();
//            if (!idToLoaded.containsKey(comment.getGitHubId()))
//            {
//                map(activity, repositoryPullRequest, comment);
//                repositoryActivityDao.saveActivity(domainRepository, activity);
//                activity.clear();
//
//            } else
//            {
//                idToLoaded.remove(comment.getGitHubId());
//
//            }
//        }
//
//        // removes comments which are not already propagated
//        for (RepositoryPullRequestCommentActivityMapping toDelete : idToLoaded.values())
//        {
//            activeObjects.delete(toDelete);
//        }

    }

    /**
     * Re-maps provided comment into the appropriate comment activity.
     *
     * @param target
     *            activity
     * @param pullRequest
     *            owner of comments
     * @param source
     *            comment
     */
    private void map(Map<String, Object> target, RepositoryPullRequestMapping pullRequest, GitHubPullRequestComment source)
    {
//        target.put(RepositoryPullRequestActivityMapping.PULL_REQUEST_ID, pullRequest.getID());
//        target.put(RepositoryPullRequestActivityMapping.REPOSITORY_ID, pullRequest.getToRepositoryId());
//
//        target.put(RepositoryPullRequestCommentActivityMapping.ENTITY_TYPE, RepositoryPullRequestCommentActivityMapping.class);
//        target.put(RepositoryPullRequestCommentActivityMapping.REMOTE_ID, source.getGitHubId());
//        target.put(RepositoryPullRequestCommentActivityMapping.LAST_UPDATED_ON, source.getCreatedAt());
//        target.put(RepositoryPullRequestCommentActivityMapping.AUTHOR, source.getCreatedBy().getLogin());
//        target.put(RepositoryPullRequestCommentActivityMapping.RAW_AUTHOR, source.getCreatedBy().getName());
//        target.put(RepositoryPullRequestCommentActivityMapping.MESSAGE, source.getText());
    }

}
