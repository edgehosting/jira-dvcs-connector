package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.PullRequestService;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubPullRequestDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestService;

/**
 * An {@link GitHubPullRequestService} implementation.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubPullRequestServiceImpl implements GitHubPullRequestService
{

    /**
     * @see #GitHubPullRequestServiceImpl(GitHubPullRequestDAO, GithubClientProvider)
     */
    private final GitHubPullRequestDAO gitHubPullRequestDAO;

    /**
     * @see #GitHubPullRequestServiceImpl(GitHubPullRequestDAO, GithubClientProvider)
     */
    private final GithubClientProvider githubClientProvider;

    /**
     * Constructor.
     * 
     * @param gitHubPullRequestDAO
     *            injected {@link GitHubPullRequestDAO} dependency.
     * @param githubClientProvider
     *            injected {@link GithubClientProvider} dependency.
     */
    public GitHubPullRequestServiceImpl(GitHubPullRequestDAO gitHubPullRequestDAO, GithubClientProvider githubClientProvider)
    {
        this.gitHubPullRequestDAO = gitHubPullRequestDAO;
        this.githubClientProvider = githubClientProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(GitHubPullRequest gitHubPullRequest)
    {
        gitHubPullRequestDAO.save(gitHubPullRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(GitHubPullRequest gitHubPullRequest)
    {
        gitHubPullRequestDAO.delete(gitHubPullRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPullRequest getById(int id)
    {
        return gitHubPullRequestDAO.getById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPullRequest getByGitHubId(long gitHubId)
    {
        return gitHubPullRequestDAO.getByGitHubId(gitHubId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GitHubPullRequest> getByRepository(GitHubRepository repository)
    {
        return gitHubPullRequestDAO.getByRepository(repository);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPullRequest fetch(GitHubRepository gitHubRepository, long gitHubId, int pullRequestNumber, Repository repository)
    {
        GitHubPullRequest result = getByGitHubId(gitHubId);
        if (result != null)
        {
            return result;

        }
        result = new GitHubPullRequest();
        RepositoryId egitRepository = RepositoryId.createFromUrl(repository.getRepositoryUrl());

        PullRequestService pullRequestService = githubClientProvider.getPullRequestService(repository);
        PullRequest loaded;
        try
        {
            loaded = pullRequestService.getPullRequest(egitRepository, pullRequestNumber);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        // re-mapping
        result.setGitHubId(loaded.getId());
        result.setRepository(gitHubRepository);
        result.setBaseRepository(gitHubRepository);
        result.setTitle(loaded.getTitle());
        result.setText(loaded.getBodyText());
        result.setUrl(loaded.getHtmlUrl());

        save(result);

        return result;
    }
}
