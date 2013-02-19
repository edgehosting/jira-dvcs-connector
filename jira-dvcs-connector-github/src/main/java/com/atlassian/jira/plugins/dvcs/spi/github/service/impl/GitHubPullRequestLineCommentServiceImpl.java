package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestLineCommentMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubPullRequestLineCommentDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestLineComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
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
     * @see #GitHubPullRequestLineCommentServiceImpl(GitHubPullRequestLineCommentDAO, RepositoryActivityDao)
     */
    private final GitHubPullRequestLineCommentDAO gitHubPullRequestLineCommentDAO;

    /**
     * @see #GitHubPullRequestLineCommentServiceImpl(GitHubPullRequestLineCommentDAO, RepositoryActivityDao)
     */
    private final RepositoryActivityDao repositoryActivityDao;

    /**
     * Constructor.
     * 
     * @param gitHubPullRequestLineCommentDAO
     *            injected {@link GitHubPullRequestLineCommentDAO} dependency
     * @param repositoryActivityDao
     *            injected {@link RepositoryActivityDao} dependency
     */
    public GitHubPullRequestLineCommentServiceImpl(GitHubPullRequestLineCommentDAO gitHubPullRequestLineCommentDAO,
            RepositoryActivityDao repositoryActivityDao)
    {
        this.gitHubPullRequestLineCommentDAO = gitHubPullRequestLineCommentDAO;
        this.repositoryActivityDao = repositoryActivityDao;
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
    public List<GitHubPullRequestLineComment> getByRepository(GitHubRepository repository)
    {
        return gitHubPullRequestLineCommentDAO.getByRepository(repository);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void synchronize(Repository domainRepository, GitHubRepository domain)
    {

        Map<String, Object> activity = new HashMap<String, Object>();
        for (GitHubPullRequestLineComment comment : getByRepository(domain))
        {
            RepositoryPullRequestMapping repositoryPullRequest = repositoryActivityDao.findRequestByRemoteId(domainRepository.getId(),
                    comment.getPullRequest().getGitHubId());
            map(activity, repositoryPullRequest, comment);
            repositoryActivityDao.saveActivity(activity);
            activity.clear();
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

        target.put(RepositoryActivityPullRequestLineCommentMapping.ENTITY_TYPE, RepositoryActivityPullRequestLineCommentMapping.class);
        target.put(RepositoryActivityPullRequestLineCommentMapping.LAST_UPDATED_ON, source.getCreatedAt());
        target.put(RepositoryActivityPullRequestLineCommentMapping.AUTHOR, source.getCreatedBy().getLogin());
        target.put(RepositoryActivityPullRequestLineCommentMapping.COMMENT_URL, source.getHtmlUrl());
        target.put(RepositoryActivityPullRequestLineCommentMapping.MESSAGE, source.getText());
    }

}
