package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryCommitCommentActivityMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryCommitMapping;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubCommitCommentDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommitComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubCommitCommentService;

/**
 * The implementation of the {@link GitHubCommitCommentService}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubCommitCommentServiceImpl implements GitHubCommitCommentService
{

    /**
     * @see #GitHubCommitCommentServiceImpl(ActiveObjects, GitHubCommitCommentDAO, RepositoryActivityDao)
     */
    private final ActiveObjects activeObjects;

    /**
     * @see #GitHubCommitCommentServiceImpl(ActiveObjects, GitHubCommitCommentDAO, RepositoryActivityDao)
     */
    private final GitHubCommitCommentDAO gitHubCommitCommentDAO;

    /**
     * @see #GitHubCommitCommentServiceImpl(ActiveObjects, GitHubCommitCommentDAO, RepositoryActivityDao)
     */
    private final RepositoryActivityDao repositoryActivityDao;

    /**
     * Constructor.
     * 
     * @param activeObjects
     *            injected {@link ActiveObjects} dependency
     * @param gitHubCommitCommentDAO
     *            injected {@link GitHubCommitComment} dependency
     * @param repositoryActivityDao
     *            injected {@link RepositoryActivityDao} dependency
     */
    public GitHubCommitCommentServiceImpl(ActiveObjects activeObjects, GitHubCommitCommentDAO gitHubCommitCommentDAO,
            RepositoryActivityDao repositoryActivityDao)
    {
        this.activeObjects = activeObjects;
        this.gitHubCommitCommentDAO = gitHubCommitCommentDAO;
        this.repositoryActivityDao = repositoryActivityDao;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(GitHubCommitComment gitHubCommitComment)
    {
        gitHubCommitCommentDAO.save(gitHubCommitComment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(GitHubCommitComment gitHubCommitComment)
    {
        gitHubCommitCommentDAO.delete(gitHubCommitComment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubCommitComment getById(int id)
    {
        return gitHubCommitCommentDAO.getById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubCommitComment getByGitHubId(long gitHubId)
    {
        return gitHubCommitCommentDAO.getByGitHubId(gitHubId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GitHubCommitComment> getAll(GitHubRepository domain, int first, int count)
    {
        return gitHubCommitCommentDAO.getAll(domain, first, count);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getAllCount(GitHubRepository domain)
    {
        return gitHubCommitCommentDAO.getAllCount(domain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void synchronize(Repository domainRepository, GitHubRepository domain)
    {
        int pageSize = 128;

        Map<String, Object> activityMap = new HashMap<String, Object>();
        List<GitHubCommit> commentedCommits;

        int i = 0;
        do
        {
            commentedCommits = gitHubCommitCommentDAO.getCommentedCommits(domain, i, pageSize);
            i += commentedCommits.size();

            for (GitHubCommit commentedCommit : commentedCommits)
            {
                RepositoryCommitMapping repositoryCommit = repositoryActivityDao
                        .getCommitByNode(domainRepository, commentedCommit.getSha());

                Map<Long, RepositoryCommitCommentActivityMapping> idToLoaded = new HashMap<Long, RepositoryCommitCommentActivityMapping>();
                for (RepositoryCommitCommentActivityMapping comment : repositoryActivityDao.getCommitComments(domainRepository,
                        repositoryCommit))
                {
                    if (StringUtils.isEmpty(comment.getFile())) {
                        idToLoaded.put(comment.getRemoteId(), comment);
                    }
                }

                for (GitHubCommitComment gitHubCommitComment : gitHubCommitCommentDAO.getByCommit(domain, commentedCommit))
                {
                    if (!idToLoaded.containsKey(gitHubCommitComment.getGitHubId()))
                    {
                        map(domainRepository, activityMap, gitHubCommitComment);
                        repositoryActivityDao.saveActivity(domainRepository, activityMap);
                        activityMap.clear();

                    } else
                    {
                        idToLoaded.remove(gitHubCommitComment.getGitHubId());
                    }
                }

                // removes previous one, which are not already propagated
                for (RepositoryCommitCommentActivityMapping delete : idToLoaded.values())
                {
                    activeObjects.delete(delete);
                }
            }
        } while (!commentedCommits.isEmpty());
    }

    /**
     * Maps provided GitHub comment into activity comment.
     * 
     * @param domainRepository
     *            over which repository
     * @param target
     *            activity
     * @param source
     *            source comment
     */
    private void map(Repository domainRepository, Map<String, Object> target, GitHubCommitComment source)
    {

        RepositoryCommitMapping commit = repositoryActivityDao.getCommitByNode(domainRepository, source.getCommit().getSha());

        target.put(RepositoryCommitCommentActivityMapping.REPOSITORY_ID, domainRepository.getId());
        target.put(RepositoryCommitCommentActivityMapping.COMMIT, commit.getID());
        target.put(RepositoryCommitCommentActivityMapping.COMMENT_URL, source.getHtmlUrl());

        target.put(RepositoryCommitCommentActivityMapping.ENTITY_TYPE, RepositoryCommitCommentActivityMapping.class);
        target.put(RepositoryCommitCommentActivityMapping.REMOTE_ID, source.getGitHubId());
        target.put(RepositoryCommitCommentActivityMapping.LAST_UPDATED_ON, source.getCreatedAt());
        target.put(RepositoryCommitCommentActivityMapping.AUTHOR, source.getCreatedBy().getLogin());
        target.put(RepositoryCommitCommentActivityMapping.RAW_AUTHOR, source.getCreatedBy().getName());
        target.put(RepositoryCommitCommentActivityMapping.MESSAGE, source.getText());
    }

}
