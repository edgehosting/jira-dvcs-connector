package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import org.eclipse.egit.github.core.CommitComment;

import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubCommitLineCommentDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommitLineComment;
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
     * @see #GitHubCommitLineCommentServiceImpl(GitHubCommitLineCommentDAO)
     */
    private final GitHubCommitLineCommentDAO gitHubCommitLineCommentDAO;

    /**
     * Constructor.
     * 
     * @param gitHubCommitLineCommentDAO
     *            injected {@link GitHubCommitLineCommentDAO} dependency
     */
    public GitHubCommitLineCommentServiceImpl(GitHubCommitLineCommentDAO gitHubCommitLineCommentDAO)
    {
        this.gitHubCommitLineCommentDAO = gitHubCommitLineCommentDAO;
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
    public void map(GitHubCommitLineComment target, CommitComment source, GitHubCommit commit)
    {
        target.setGitHubId(source.getId());
        target.setCommit(commit);
        target.setPath(source.getPath());
        target.setLine(source.getLine());
        target.setText(source.getBodyText());
    }
}
