package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import java.util.List;
import java.util.Map;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubCommitLineCommentDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommitLineComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubCommitLineCommentService;

/**
 * The {@link GitHubCommitLineCommentService} implementation.
 *
 * @author Stanislav Dvorscak
 *
 */
public class GitHubCommitLineCommentServiceImpl implements GitHubCommitLineCommentService
{

    /**
     * @see #GitHubCommitLineCommentServiceImpl(ActiveObjects, GitHubCommitLineCommentDAO, RepositoryActivityDao)
     */
    private final GitHubCommitLineCommentDAO gitHubCommitLineCommentDAO;

    /**
     * @see #GitHubCommitLineCommentServiceImpl(ActiveObjects, GitHubCommitLineCommentDAO, RepositoryActivityDao)
     */
    private final RepositoryActivityDao repositoryActivityDao;

    /**
     * @see #GitHubCommitLineCommentServiceImpl(ActiveObjects, GitHubCommitLineCommentDAO, RepositoryActivityDao)
     */
    private final ActiveObjects activeObjects;

    /**
     * Constructor.
     *
     * @param activeObjects
     *            injected {@link ActiveObjects} dependency
     * @param gitHubCommitLineCommentDAO
     *            injected {@link GitHubCommitLineCommentDAO} dependency
     * @param repositoryActivityDao
     *            injected {@link RepositoryActivityDao} dependency
     */
    public GitHubCommitLineCommentServiceImpl(ActiveObjects activeObjects, GitHubCommitLineCommentDAO gitHubCommitLineCommentDAO,
            RepositoryActivityDao repositoryActivityDao)
    {
        this.activeObjects = activeObjects;
        this.gitHubCommitLineCommentDAO = gitHubCommitLineCommentDAO;
        this.repositoryActivityDao = repositoryActivityDao;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(GitHubCommitLineComment gitHubCommitLineComment)
    {
        gitHubCommitLineCommentDAO.save(gitHubCommitLineComment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(GitHubCommitLineComment gitHubCommitLineComment)
    {
        gitHubCommitLineCommentDAO.delete(gitHubCommitLineComment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubCommitLineComment getById(int id)
    {
        return gitHubCommitLineCommentDAO.getById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubCommitLineComment getByGitHubId(long gitHubId)
    {
        return gitHubCommitLineCommentDAO.getByGitHubId(gitHubId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GitHubCommitLineComment> getAll(GitHubRepository domain, int first, int count)
    {
        return gitHubCommitLineCommentDAO.getAll(domain, first, count);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getAllCount(GitHubRepository domain)
    {
        return gitHubCommitLineCommentDAO.getAllCount(domain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void synchronize(Repository domainRepository, GitHubRepository domain)
    {
//        int pageSize = 128;
//
//        Map<String, Object> activityMap = new HashMap<String, Object>();
//        List<GitHubCommit> commentedCommits;
//
//        int i = 0;
//        do
//        {
//            commentedCommits = gitHubCommitLineCommentDAO.getCommentedCommits(domain, i, pageSize);
//            i += commentedCommits.size();
//
//            for (GitHubCommit commentedCommit : commentedCommits)
//            {
//                RepositoryCommitMapping repositoryCommit = repositoryActivityDao
//                        .getCommitByNode(domainRepository, commentedCommit.getSha());
//
//                Map<Long, RepositoryCommitCommentActivityMapping> idToLoaded = new HashMap<Long, RepositoryCommitCommentActivityMapping>();
//                for (RepositoryCommitCommentActivityMapping comment : repositoryActivityDao.getCommitComments(domainRepository,
//                        repositoryCommit))
//                {
//                    if (!StringUtils.isEmpty(comment.getFile())) {
//                        idToLoaded.put(comment.getRemoteId(), comment);
//                    }
//                }
//
//                for (GitHubCommitLineComment gitHubCommitLineComment : gitHubCommitLineCommentDAO.getByCommit(domain, commentedCommit))
//                {
//                    if (!idToLoaded.containsKey(gitHubCommitLineComment.getGitHubId()))
//                    {
//                        map(domainRepository, activityMap, gitHubCommitLineComment);
//                        repositoryActivityDao.saveActivity(domainRepository, activityMap);
//                        activityMap.clear();
//
//                    } else
//                    {
//                        idToLoaded.remove(gitHubCommitLineComment.getGitHubId());
//                    }
//                }
//
//                // removes previous one, which are not already propagated
//                for (RepositoryCommitCommentActivityMapping delete : idToLoaded.values())
//                {
//                    activeObjects.delete(delete);
//                }
//            }
//        } while (!commentedCommits.isEmpty());
    }

    /**
     * Maps provided GitHub comment into an activity comment.
     *
     * @param domainRepository
     *            over which repository
     * @param target
     *            activity
     * @param source
     *            source comment
     */
    private void map(Repository domainRepository, Map<String, Object> target, GitHubCommitLineComment source)
    {
//        RepositoryCommitMapping commit = repositoryActivityDao.getCommitByNode(domainRepository, source.getCommit().getSha());
//        target.put(RepositoryCommitCommentActivityMapping.REPOSITORY_ID, domainRepository.getId());
//        target.put(RepositoryCommitCommentActivityMapping.COMMIT, commit.getID());
//        target.put(RepositoryCommitCommentActivityMapping.COMMENT_URL, source.getHtmlUrl());
//
//        target.put(RepositoryCommitCommentActivityMapping.ENTITY_TYPE, RepositoryCommitCommentActivityMapping.class);
//        target.put(RepositoryCommitCommentActivityMapping.REMOTE_ID, source.getGitHubId());
//        target.put(RepositoryCommitCommentActivityMapping.LAST_UPDATED_ON, source.getCreatedAt());
//        target.put(RepositoryCommitCommentActivityMapping.AUTHOR, source.getCreatedBy().getLogin());
//        target.put(RepositoryCommitCommentActivityMapping.RAW_AUTHOR, source.getCreatedBy().getName());
//        target.put(RepositoryCommitCommentActivityMapping.FILE, source.getPath());
//        target.put(RepositoryCommitCommentActivityMapping.MESSAGE, source.getText());
    }

}
