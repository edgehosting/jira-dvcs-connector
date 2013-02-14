package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import java.util.List;

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
     * @see #GitHubPullRequestLineCommentServiceImpl(GitHubPullRequestLineCommentDAO)
     */
    private final GitHubPullRequestLineCommentDAO gitHubPullRequestLineCommentDAO;

    /**
     * Constructor.
     * 
     * @param gitHubPullRequestLineCommentDAO
     *            Injected {@link GitHubPullRequestLineCommentDAO} dependency.
     */
    public GitHubPullRequestLineCommentServiceImpl(GitHubPullRequestLineCommentDAO gitHubPullRequestLineCommentDAO)
    {
        this.gitHubPullRequestLineCommentDAO = gitHubPullRequestLineCommentDAO;
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

}
