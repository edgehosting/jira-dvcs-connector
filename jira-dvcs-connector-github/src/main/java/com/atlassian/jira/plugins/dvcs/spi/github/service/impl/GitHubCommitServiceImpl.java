package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.CommitUser;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.CommitService;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.GitHubUtils;
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
    public GitHubCommit getBySha(GitHubRepository domain, GitHubRepository repository, String sha)
    {
        return gitHubCommitDAO.getBySha(domain, repository, sha);
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
    public GitHubCommit fetch(Repository domainRepository, GitHubRepository domain, GitHubRepository repository, String sha)
    {
        GitHubCommit result = getBySha(domain, repository, sha);
        if (result != null)
        {
            return result;
        }

        CommitService commitService = githubClientProvider.getCommitService(domainRepository);
        IRepositoryIdProvider egitRepository = RepositoryId.createFromUrl(repository.getUrl());

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

        CommitUser author = commit.getCommit().getAuthor();

        result = new GitHubCommit();
        result.setDomain(domain);
        result.setRepository(repository);
        result.setSha(commit.getSha());
        result.setCreatedAt(author.getDate());
        result.setCreatedBy(author.getName());
        result.setCreatedByName(author.getName());
        result.setCreatedByAvatarUrl(commit.getCommitter() != null ? commit.getCommitter().getAvatarUrl() : null);
        result.setHtmlUrl(GitHubUtils.getHtmlUrlCommit(domainRepository.getOrgName(), domainRepository.getSlug(), sha));
        result.setMessage(commit.getCommit().getMessage());
        save(result);

        return result;
    }
}
