package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import java.util.List;
import java.util.Map;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.Progress;
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
    public void synchronize(Repository domainRepository, GitHubPullRequest pullRequest, Progress progress)
    {
        if (progress.isShouldStop())
        {
            return;
        }

//        RepositoryPullRequestMapping repositoryPullRequest = repositoryActivityDao.findRequestByRemoteId(domainRepository,
//                pullRequest.getGitHubId());
//
//        Map<Long, RepositoryPullRequestCommentActivityMapping> idToLoaded = new HashMap<Long, RepositoryPullRequestCommentActivityMapping>();
//        for (RepositoryPullRequestCommentActivityMapping loaded : repositoryActivityDao.getPullRequestComments(domainRepository,
//                repositoryPullRequest))
//        {
//            if (!StringUtils.isEmpty(loaded.getFile()))
//            {
//                idToLoaded.put(loaded.getRemoteId(), loaded);
//            }
//        }
//
//        Map<String, Object> activity = new HashMap<String, Object>();
//        for (GitHubPullRequestLineComment comment : getByPullRequest(pullRequest))
//        {
//            if (progress.isShouldStop())
//            {
//                return;
//            }
//
//            if (!idToLoaded.containsKey(comment.getGitHubId()))
//            {
//                map(activity, repositoryPullRequest, comment);
//                repositoryActivityDao.saveActivity(domainRepository, activity);
//                activity.clear();
//            } else
//            {
//                idToLoaded.remove(comment.getGitHubId());
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
     *            owner of comment
     * @param source
     *            comment
     */
    private void map(Map<String, Object> target, RepositoryPullRequestMapping pullRequest, GitHubPullRequestLineComment source)
    {
//        target.put(RepositoryPullRequestActivityMapping.PULL_REQUEST_ID, pullRequest.getID());
//        target.put(RepositoryPullRequestActivityMapping.REPOSITORY_ID, pullRequest.getToRepositoryId());
//
//        target.put(RepositoryPullRequestCommentActivityMapping.ENTITY_TYPE, RepositoryPullRequestCommentActivityMapping.class);
//        target.put(RepositoryPullRequestCommentActivityMapping.REMOTE_ID, source.getGitHubId());
//        target.put(RepositoryPullRequestCommentActivityMapping.LAST_UPDATED_ON, source.getCreatedAt());
//        target.put(RepositoryPullRequestCommentActivityMapping.AUTHOR, source.getCreatedBy().getLogin());
//        target.put(RepositoryPullRequestCommentActivityMapping.RAW_AUTHOR, source.getCreatedBy().getName());
//        target.put(RepositoryPullRequestCommentActivityMapping.COMMENT_URL, source.getHtmlUrl());
//        target.put(RepositoryPullRequestCommentActivityMapping.MESSAGE, source.getText());
//        target.put(RepositoryPullRequestCommentActivityMapping.FILE, source.getPath());
    }

}
