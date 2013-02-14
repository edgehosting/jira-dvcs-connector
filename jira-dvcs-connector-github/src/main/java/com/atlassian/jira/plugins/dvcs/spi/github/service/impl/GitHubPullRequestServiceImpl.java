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
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubRepositoryService;

/**
 * An {@link GitHubPullRequestService} implementation.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubPullRequestServiceImpl implements GitHubPullRequestService
{

    /**
     * @see #GitHubPullRequestServiceImpl(GitHubPullRequestDAO, GitHubRepositoryService, GithubClientProvider)
     */
    private final GitHubPullRequestDAO gitHubPullRequestDAO;

    /**
     * @see #GitHubPullRequestServiceImpl(GitHubPullRequestDAO, GitHubRepositoryService, GithubClientProvider)
     */
    private final GitHubRepositoryService gitHubRepositoryService;

    /**
     * @see #GitHubPullRequestServiceImpl(GitHubPullRequestDAO, GitHubRepositoryService, GithubClientProvider)
     */
    private final GithubClientProvider githubClientProvider;

    /**
     * Constructor.
     * 
     * @param gitHubPullRequestDAO
     *            injected {@link GitHubPullRequestDAO} dependency
     * @param injected
     *            {@link GitHubRepositoryService} dependency
     * @param githubClientProvider
     *            injected {@link GithubClientProvider} dependency
     */
    public GitHubPullRequestServiceImpl(GitHubPullRequestDAO gitHubPullRequestDAO, GitHubRepositoryService gitHubRepositoryService,
            GithubClientProvider githubClientProvider)
    {
        this.gitHubPullRequestDAO = gitHubPullRequestDAO;
        this.githubClientProvider = githubClientProvider;
        this.gitHubRepositoryService = gitHubRepositoryService;
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
    public GitHubPullRequest fetch(Repository domainRepository, GitHubRepository domain, long gitHubId, int pullRequestNumber)
    {
        GitHubPullRequest result = getByGitHubId(gitHubId);
        if (result != null)
        {
            return result;

        }
        result = new GitHubPullRequest();

        PullRequestService pullRequestService = githubClientProvider.getPullRequestService(domainRepository);
        PullRequest loaded;
        try
        {
            loaded = pullRequestService.getPullRequest(RepositoryId.createFromUrl(domain.getUrl()), pullRequestNumber);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        org.eclipse.egit.github.core.Repository baseRepo = loaded.getBase().getRepo();
        GitHubRepository baseRepository = gitHubRepositoryService.fetch(domainRepository, baseRepo.getOwner().getLogin(),
                baseRepo.getName(), baseRepo.getId());

        org.eclipse.egit.github.core.Repository headRepo = loaded.getHead().getRepo();
        GitHubRepository headRepository = gitHubRepositoryService.fetch(domainRepository, headRepo.getOwner().getLogin(),
                headRepo.getName(), headRepo.getId());

        // re-mapping
        result.setGitHubId(loaded.getId());
        result.setDomain(domain);
        result.setNumber(loaded.getNumber());
        result.setBaseRepository(baseRepository);
        result.setBaseSha(loaded.getBase().getSha());
        result.setHeadRepository(headRepository);
        result.setHeadSha(loaded.getHead().getSha());
        result.setTitle(loaded.getTitle());
        result.setText(loaded.getBodyText());
        result.setUrl(loaded.getHtmlUrl());

        save(result);

        return result;
    }
}
