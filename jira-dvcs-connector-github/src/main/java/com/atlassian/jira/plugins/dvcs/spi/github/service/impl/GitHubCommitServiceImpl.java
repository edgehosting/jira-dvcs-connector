package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.CommitService;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubCommitDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
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
     * @see #
     */
    private final GithubClientProvider githubClientProvider;

    /**
     * Constructor.
     * 
     * @param gitHubCommitDAO
     *            injected {@link GitHubCommitDAO} dependency
     * @param githubClientProvider
     *            injected {@link GithubClientProvider} dependency
     */
    public GitHubCommitServiceImpl(GitHubCommitDAO gitHubCommitDAO, GithubClientProvider githubClientProvider)
    {
        this.gitHubCommitDAO = gitHubCommitDAO;
        this.githubClientProvider = githubClientProvider;
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
    public GitHubCommit fetch(GitHubRepository gitHubRepository, Repository repository, String sha)
    {
        GitHubCommit result = getBySha(sha);
        if (result != null)
        {
            return result;
        }

        CommitService commitService = githubClientProvider.getCommitService(repository);
        IRepositoryIdProvider egitRepository = RepositoryId.createFromUrl(repository.getRepositoryUrl());

        RepositoryCommit commit;
        try
        {
            commit = commitService.getCommit(egitRepository, sha);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        if (commit == null)
        {
            return null;
        }

        result = new GitHubCommit();
        result.setRepository(gitHubRepository);
        result.setSha(commit.getSha());
        result.setCreatedAt(commit.getCommit().getAuthor().getDate());
        result.setCreatedBy(commit.getCommit().getAuthor().getName());
        result.setMessage(commit.getCommit().getMessage());
        save(result);

        return result;
    }
}
