package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestCommentMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubPullRequestCommentDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
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
     * Constructor.
     * 
     * @param gitHubPullRequestCommentDAO
     *            injected {@link GitHubPullRequestCommentDAO} dependency
     * @param repositoryActivityDao
     *            injected {@link RepositoryActivityDao} dependency
     */
    public GitHubPullRequestCommentServiceImpl(GitHubPullRequestCommentDAO gitHubPullRequestCommentDAO,
            RepositoryActivityDao repositoryActivityDao)
    {
        this.gitHubPullRequestCommentDAO = gitHubPullRequestCommentDAO;
        this.repositoryActivityDao = repositoryActivityDao;
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
    public List<GitHubPullRequestComment> getByRepository(GitHubRepository repository)
    {
        return gitHubPullRequestCommentDAO.getByRepository(repository);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void synchronize(Repository domainRepository, GitHubRepository domain)
    {

        Map<String, Object> activity = new HashMap<String, Object>();
        for (GitHubPullRequestComment comment : getByRepository(domain))
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
     *            owner of comments
     * @param source
     *            comment
     */
    private void map(Map<String, Object> target, RepositoryPullRequestMapping pullRequest, GitHubPullRequestComment source)
    {
        target.put(RepositoryActivityPullRequestMapping.PULL_REQUEST_ID, pullRequest.getID());
        target.put(RepositoryActivityPullRequestMapping.REPOSITORY_ID, pullRequest.getToRepositoryId());

        target.put(RepositoryActivityPullRequestCommentMapping.ENTITY_TYPE, RepositoryActivityPullRequestCommentMapping.class);
        target.put(RepositoryActivityPullRequestCommentMapping.LAST_UPDATED_ON, source.getCreatedAt());
        target.put(RepositoryActivityPullRequestCommentMapping.AUTHOR, source.getCreatedBy().getLogin());
        target.put(RepositoryActivityPullRequestCommentMapping.MESSAGE, source.getText());
    }

}
