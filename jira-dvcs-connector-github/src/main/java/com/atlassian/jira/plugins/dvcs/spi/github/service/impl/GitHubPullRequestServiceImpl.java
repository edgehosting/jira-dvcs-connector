package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.PullRequestService;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestUpdateMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubPullRequestDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestAction;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubRepositoryService;
import com.atlassian.jira.plugins.dvcs.util.IssueKeyExtractor;

/**
 * An {@link GitHubPullRequestService} implementation.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubPullRequestServiceImpl implements GitHubPullRequestService
{

    /**
     * @see #GitHubPullRequestServiceImpl(GitHubPullRequestDAO, GitHubRepositoryService, GithubClientProvider, RepositoryActivityDao)
     */
    private final GitHubPullRequestDAO gitHubPullRequestDAO;

    /**
     * @see #GitHubPullRequestServiceImpl(GitHubPullRequestDAO, GitHubRepositoryService, GithubClientProvider, RepositoryActivityDao)
     */
    private final GitHubRepositoryService gitHubRepositoryService;

    /**
     * @see #GitHubPullRequestServiceImpl(GitHubPullRequestDAO, GitHubRepositoryService, GithubClientProvider, RepositoryActivityDao)
     */
    private final GithubClientProvider githubClientProvider;

    /**
     * @see #GitHubPullRequestServiceImpl(GitHubPullRequestDAO, GitHubRepositoryService, GithubClientProvider, RepositoryActivityDao)
     */
    private final RepositoryActivityDao repositoryActivityDao;

    /**
     * Constructor.
     * 
     * @param gitHubPullRequestDAO
     *            injected {@link GitHubPullRequestDAO} dependency
     * @param injected
     *            {@link GitHubRepositoryService} dependency
     * @param githubClientProvider
     *            injected {@link GithubClientProvider} dependency
     * @param repositoryActivityDao
     *            injected {@link RepositoryActivityDao} dependency
     */
    public GitHubPullRequestServiceImpl(GitHubPullRequestDAO gitHubPullRequestDAO, GitHubRepositoryService gitHubRepositoryService,
            GithubClientProvider githubClientProvider, RepositoryActivityDao repositoryActivityDao)
    {
        this.gitHubPullRequestDAO = gitHubPullRequestDAO;
        this.gitHubRepositoryService = gitHubRepositoryService;
        this.githubClientProvider = githubClientProvider;
        this.repositoryActivityDao = repositoryActivityDao;
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
    public GitHubPullRequestAction getOpenAction(GitHubPullRequest pullRequest)
    {
        for (GitHubPullRequestAction action : pullRequest.getActions())
        {
            if (GitHubPullRequestAction.Action.OPENED.equals(action.getAction()))
            {
                return action;
            }
        }

        throw new IllegalStateException("Unable to find open action, for provided pull request ID: " + pullRequest.getId());
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
        result.setHeadRef(loaded.getHead().getRef());
        result.setHeadSha(loaded.getHead().getSha());
        result.setTitle(loaded.getTitle());
        result.setText(loaded.getBodyText());
        result.setUrl(loaded.getHtmlUrl());

        save(result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void synchronize(Repository domainRepository, GitHubRepository domain)
    {
        Map<String, Object> repositoryPullRequestParams = new HashMap<String, Object>();
        Map<String, Object> activity = new HashMap<String, Object>();

        RepositoryPullRequestMapping repositoryPullRequest;
        for (GitHubPullRequest pullRequest : getByRepository(domain))
        {
            repositoryPullRequest = repositoryActivityDao.findRequestByRemoteId(domainRepository.getId(), pullRequest.getGitHubId());
            if (repositoryPullRequest == null)
            {
                // saves pull request
                map(domainRepository, repositoryPullRequestParams, pullRequest);
                repositoryPullRequest = repositoryActivityDao.savePullRequest(repositoryPullRequestParams,
                        IssueKeyExtractor.extractIssueKeys(pullRequest.getTitle(), pullRequest.getText()));
                repositoryPullRequestParams.clear();
            }

            // saves pull request activities
            for (GitHubPullRequestAction action : pullRequest.getActions())
            {
                if (repositoryActivityDao.getPullRequestActivityByRemoteId(repositoryPullRequest, action.getGitHubEventId()) == null) {
                    map(activity, repositoryPullRequest, action);
                    repositoryActivityDao.saveActivity(activity);
                }
            }
            activity.clear();
        }
    }

    /**
     * Re-maps provided pull request to repository pull request.
     * 
     * @param domainRepository
     *            domain repository
     * @param target
     *            repository pull request
     * @param source
     *            pull request
     */
    private void map(Repository domainRepository, Map<String, Object> target, GitHubPullRequest source)
    {
        target.put(RepositoryPullRequestMapping.REMOTE_ID, source.getGitHubId());
        target.put(RepositoryPullRequestMapping.URL, source.getUrl());
        target.put(RepositoryPullRequestMapping.NAME, source.getTitle());
        target.put(RepositoryPullRequestMapping.DESCRIPTION, source.getText());
        target.put(RepositoryPullRequestMapping.TO_REPO_ID, domainRepository.getId());
        //TODO save url in the correct format
        target.put(RepositoryPullRequestMapping.SOURCE_URL, source.getHeadRepository().getUrl());
    }

    /**
     * Re-maps provided pull request action to the pull request.
     * 
     * @param target
     *            activity
     * @param pullRequest
     *            for which pull request is this action
     * @param source
     *            action
     */
    private void map(Map<String, Object> target, RepositoryPullRequestMapping pullRequest, GitHubPullRequestAction source)
    {
        target.put(RepositoryActivityPullRequestMapping.PULL_REQUEST_ID, pullRequest.getID());
        target.put(RepositoryActivityPullRequestMapping.REPOSITORY_ID, pullRequest.getToRepositoryId());

        target.put(RepositoryActivityPullRequestMapping.ENTITY_TYPE, RepositoryActivityPullRequestUpdateMapping.class);
        target.put(RepositoryActivityPullRequestMapping.LAST_UPDATED_ON, source.getCreatedAt());
        target.put(RepositoryActivityPullRequestMapping.AUTHOR, source.getCreatedBy().getLogin());
        target.put(RepositoryActivityPullRequestMapping.RAW_AUTHOR, source.getCreatedBy().getName());
        target.put(RepositoryActivityPullRequestUpdateMapping.REMOTE_ID, source.getGitHubEventId());
        target.put(RepositoryActivityPullRequestUpdateMapping.STATUS, resolveStatus(source));
    }

    /**
     * @param action
     * @return resolved status
     */
    private RepositoryActivityPullRequestUpdateMapping.Status resolveStatus(GitHubPullRequestAction action)
    {
        RepositoryActivityPullRequestUpdateMapping.Status result = null;

        if (GitHubPullRequestAction.Action.OPENED.equals(action.getAction()))
        {
            result = RepositoryActivityPullRequestUpdateMapping.Status.OPENED;

        } else if (GitHubPullRequestAction.Action.MERGED.equals(action.getAction()))
        {
            result = RepositoryActivityPullRequestUpdateMapping.Status.MERGED;

        } else if (GitHubPullRequestAction.Action.CLOSED.equals(action.getAction()))
        {
            result = RepositoryActivityPullRequestUpdateMapping.Status.DECLINED;

        } else if (GitHubPullRequestAction.Action.REOPENED.equals(action.getAction()))
        {
            result = RepositoryActivityPullRequestUpdateMapping.Status.REOPENED;

        }

        return result;
    }

}
