package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import java.util.List;

import org.eclipse.egit.github.core.Commit;

import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubCommitDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubCommitService;

/**
 * An {@link GitHubCommitService} implementation.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubCommitServiceImpl implements GitHubCommitService
{

    /**
     * @see #GitHubCommitServiceImpl(GitHubCommitDAO)
     */
    private final GitHubCommitDAO gitHubCommitDAO;

    /**
     * Constructor.
     * 
     * @param gitHubCommitDAO
     *            injected {@link GitHubCommitDAO} dependency
     */
    public GitHubCommitServiceImpl(GitHubCommitDAO gitHubCommitDAO)
    {
        this.gitHubCommitDAO = gitHubCommitDAO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(GitHubCommit gitHubCommit)
    {
        gitHubCommitDAO.save(gitHubCommit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(GitHubCommit gitHubCommit)
    {
        gitHubCommitDAO.delete(gitHubCommit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubCommit getById(int id)
    {
        return gitHubCommitDAO.getById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubCommit getBySha(String sha)
    {
        return gitHubCommitDAO.getBySha(sha);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GitHubCommit> getByIssueKey(String issueKey)
    {
        return gitHubCommitDAO.getByIssueKey(issueKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void map(GitHubCommit target, Commit source)
    {
        target.setSha(source.getSha());
        target.setCreatedAt(source.getAuthor().getDate());
        target.setCreatedBy(source.getAuthor().getName());
        target.setMessage(source.getMessage());
    }

}
