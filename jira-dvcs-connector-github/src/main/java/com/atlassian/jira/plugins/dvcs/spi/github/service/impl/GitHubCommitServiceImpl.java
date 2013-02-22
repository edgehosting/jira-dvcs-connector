package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.CommitUser;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityCommitMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestUpdateMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.GitHubUtils;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubCommitDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestAction;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubCommitService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestService;

/**
 * An {@link GitHubCommitService} implementation.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubCommitServiceImpl implements GitHubCommitService
{

    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubCommitServiceImpl.class);

    /**
     * @see #GitHubCommitServiceImpl(GitHubCommitDAO, GitHubPullRequestService, GithubClientProvider, RepositoryActivityDao, ActiveObjects)
     */
    private final GitHubCommitDAO gitHubCommitDAO;

    /**
     * @see #GitHubCommitServiceImpl(GitHubCommitDAO, GitHubPullRequestService, GithubClientProvider, RepositoryActivityDao, ActiveObjects)
     */
    private final GitHubPullRequestService gitHubPullRequestService;

    /**
     * @see #GitHubCommitServiceImpl(GitHubCommitDAO, GitHubPullRequestService, GithubClientProvider, RepositoryActivityDao, ActiveObjects)
     */
    private final RepositoryActivityDao repositoryActivityDao;

    /**
     * @see #GitHubCommitServiceImpl(GitHubCommitDAO, GitHubPullRequestService, GithubClientProvider, RepositoryActivityDao, ActiveObjects)
     */
    private final GithubClientProvider githubClientProvider;

    /**
     * @see #GitHubCommitServiceImpl(GitHubCommitDAO, GitHubPullRequestService, GithubClientProvider, RepositoryActivityDao, ActiveObjects)
     */
    private final ActiveObjects activeObjects;

    /**
     * Constructor.
     * 
     * @param gitHubCommitDAO
     *            injected {@link GitHubCommitDAO} dependency
     * @param gitHubPullRequestService
     *            injected {@link GitHubPullRequestService} dependency
     * @param githubClientProvider
     *            injected {@link GithubClientProvider} dependency
     * @param repositoryActivityDao
     *            {@link RepositoryActivityDao} dependency
     * @param activeObjects
     *            injected {@link ActiveObjects} dependency
     */
    public GitHubCommitServiceImpl(GitHubCommitDAO gitHubCommitDAO, GitHubPullRequestService gitHubPullRequestService,
            GithubClientProvider githubClientProvider, RepositoryActivityDao repositoryActivityDao, ActiveObjects activeObjects)
    {
        this.gitHubCommitDAO = gitHubCommitDAO;
        this.gitHubPullRequestService = gitHubPullRequestService;
        this.githubClientProvider = githubClientProvider;
        this.repositoryActivityDao = repositoryActivityDao;
        this.activeObjects = activeObjects;
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
        User committer = commit.getCommitter();

        result = new GitHubCommit();
        result.setDomain(domain);
        result.setRepository(repository);
        result.setSha(commit.getSha());
        result.setCreatedAt(author.getDate());
        result.setCreatedBy(committer != null ? committer.getLogin() : author.getName());
        result.setCreatedByName(author.getName());
        result.setCreatedByAvatarUrl(committer != null ? committer.getAvatarUrl() : null);
        result.setHtmlUrl(GitHubUtils.getHtmlUrlCommit(domainRepository.getOrgName(), domainRepository.getSlug(), sha));
        result.setMessage(commit.getCommit().getMessage());
        save(result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void synchronize(Repository domainRepository, GitHubRepository domain, GitHubPullRequest pullRequest)
    {
        List<GitHubCommit> allCommits = pullRequest.getCommits();
        GitHubCommit lastCommit = !allCommits.isEmpty() ? allCommits.get(allCommits.size() - 1) : null;

        // was head changed? is necessary to refresh commits?
        if (lastCommit == null || !lastCommit.getSha().equals(pullRequest.getHeadSha()))
        {
            fetch(domainRepository, domain, pullRequest);
        }

        synchronizeOpenCommits(pullRequest, domainRepository);
        synchronizeUpdateCommits(pullRequest, domainRepository);
    }

    /**
     * Fetch all new commits for provided pull request.
     * 
     * @param domainRepository
     *            for repository
     * @param domain
     *            for repository
     * @param pullRequest
     *            commits owner
     */
    private void fetch(Repository domainRepository, GitHubRepository domain, GitHubPullRequest pullRequest)
    {
        pullRequest.getCommits().clear();

        PullRequestService pullRequestService = githubClientProvider.getPullRequestService(domainRepository);
        try
        {
            List<RepositoryCommit> repositoryCommits = pullRequestService.getCommits(
                    RepositoryId.createFromUrl(domainRepository.getRepositoryUrl()), pullRequest.getNumber());
            for (RepositoryCommit repositoryCommit : repositoryCommits)
            {
                pullRequest.getCommits().add(fetch(domainRepository, domain, pullRequest.getHeadRepository(), repositoryCommit.getSha()));
            }
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        gitHubPullRequestService.save(pullRequest);
    }

    /**
     * Synchronizes pull request open commits information.
     * 
     * @param pullRequest
     *            commits owner
     * @param domainRepository 
     */
    private void synchronizeOpenCommits(GitHubPullRequest pullRequest, Repository domainRepository)
    {
        RepositoryPullRequestMapping repositoryPullRequest = repositoryActivityDao.findRequestByRemoteId(
                domainRepository.getId(), pullRequest.getGitHubId());
        List<RepositoryActivityPullRequestUpdateMapping> opened = repositoryActivityDao.getPullRequestActivityByStatus(
                repositoryPullRequest, RepositoryActivityPullRequestUpdateMapping.Status.OPENED);

        RepositoryActivityPullRequestUpdateMapping openActivity;
        if (opened.size() == 1)
        {
            openActivity = opened.get(0);

        } else if (opened.size() > 1)
        {
            LOGGER.error("There are multiple open activities for provided pull request, RepositoryPullRequestID: "
                    + repositoryPullRequest.getID() + " Founded records: " + opened.size() + " First one will be used!");
            openActivity = opened.get(0);

        } else
        {
            throw new IllegalStateException("Unable to find open activity for provided pull request, RepositoryPullRequest ID: "
                    + repositoryPullRequest.getID());
        }

        // SHA to already stored commit
        Map<String, RepositoryActivityCommitMapping> loadedCommits = new HashMap<String, RepositoryActivityCommitMapping>();
        for (RepositoryActivityCommitMapping loadedCommit : openActivity.getCommits())
        {
            loadedCommits.put(loadedCommit.getNode(), loadedCommit);
        }

        Iterator<GitHubCommit> commitsIterator = pullRequest.getCommits().iterator();
        GitHubPullRequestAction openAction = gitHubPullRequestService.getOpenAction(pullRequest);

        GitHubCommit cursor;
        do
        {
            cursor = commitsIterator.next();

            if (!loadedCommits.containsKey(cursor.getSha()))
            {
                Map<String, Object> commit = new HashMap<String, Object>();
                map(commit, openActivity, cursor);
                repositoryActivityDao.saveCommit(commit);

            } else
            {
                loadedCommits.remove(cursor.getSha());

            }

        } while (!cursor.getSha().equals(openAction.getHeadSha()));

        // deletes loaded commits, which are not already propagated
        for (RepositoryActivityCommitMapping toDelete : loadedCommits.values())
        {
            activeObjects.delete(toDelete);
        }
    }

    /**
     * Synchronizes pull request update commits information.
     * 
     * @param pullRequest
     *            commits owner
     * @param domainRepository 
     */
    private void synchronizeUpdateCommits(GitHubPullRequest pullRequest, Repository domainRepository)
    {
        GitHubCommit lastCommit = pullRequest.getCommits().isEmpty() ? null : pullRequest.getCommits().get(
                pullRequest.getCommits().size() - 1);
        if (lastCommit == null)
        {
            return;
        }

        RepositoryPullRequestMapping repositoryPullRequest = repositoryActivityDao.findRequestByRemoteId(
                domainRepository.getId(), pullRequest.getGitHubId());
        List<RepositoryActivityPullRequestUpdateMapping> updated = repositoryActivityDao.getPullRequestActivityByStatus(
                repositoryPullRequest, RepositoryActivityPullRequestUpdateMapping.Status.UPDATED);

        RepositoryActivityPullRequestUpdateMapping updateActivity;
        if (updated.isEmpty())
        {
            Map<String, Object> updateActivityParams = new HashMap<String, Object>();
            updateActivityParams.put(RepositoryActivityPullRequestMapping.PULL_REQUEST_ID, repositoryPullRequest.getID());
            updateActivityParams.put(RepositoryActivityPullRequestMapping.REPOSITORY_ID, repositoryPullRequest.getToRepositoryId());
            updateActivityParams.put(RepositoryActivityPullRequestMapping.ENTITY_TYPE, RepositoryActivityPullRequestUpdateMapping.class);
            updateActivityParams.put(RepositoryActivityPullRequestMapping.LAST_UPDATED_ON, lastCommit.getCreatedAt());
            updateActivityParams.put(RepositoryActivityPullRequestMapping.AUTHOR, lastCommit.getCreatedBy());
            updateActivityParams.put(RepositoryActivityPullRequestMapping.RAW_AUTHOR, lastCommit.getCreatedByName());
            updateActivityParams.put(RepositoryActivityPullRequestUpdateMapping.STATUS,
                    RepositoryActivityPullRequestUpdateMapping.Status.UPDATED);
            updateActivity = (RepositoryActivityPullRequestUpdateMapping) repositoryActivityDao.saveActivity(updateActivityParams);

        } else if (updated.size() == 1)
        {
            updateActivity = updated.get(0);

        } else
        {
            updateActivity = updated.get(0);
            LOGGER.error("Currently only one GitHub UPDATED activity per pull request is supported! There was founded: " + updated.size()
                    + " activities, for Repository Pull Request ID: " + repositoryPullRequest.getID() + " The first one will be used!");

        }

        // SHA to already stored commit
        Map<String, RepositoryActivityCommitMapping> loadedCommits = new HashMap<String, RepositoryActivityCommitMapping>();
        for (RepositoryActivityCommitMapping loadedCommit : updateActivity.getCommits())
        {
            loadedCommits.put(loadedCommit.getNode(), loadedCommit);
        }

        Iterator<GitHubCommit> commitsIterator = pullRequest.getCommits().iterator();
        GitHubPullRequestAction openAction = gitHubPullRequestService.getOpenAction(pullRequest);

        // skips open commits
        while (!commitsIterator.next().getSha().equals(openAction.getHeadSha()))
            ;

        //
        GitHubCommit cursor;
        while (commitsIterator.hasNext())
        {
            cursor = commitsIterator.next();

            if (!loadedCommits.containsKey(cursor.getSha()))
            {
                Map<String, Object> commit = new HashMap<String, Object>();
                map(commit, updateActivity, cursor);
                repositoryActivityDao.saveCommit(commit);
            } else
            {
                loadedCommits.remove(cursor.getSha());
            }
        }

        // deletes loaded commits, which are not already propagated
        for (RepositoryActivityCommitMapping toDelete : loadedCommits.values())
        {
            activeObjects.delete(toDelete);
        }

    }

    /**
     * Re-maps provided model value into the {@link RepositoryActivityCommitMapping} AO creation map.
     * 
     * @param target
     * @param activity
     *            to which is linked this commit
     * @param source
     */
    private void map(Map<String, Object> target, RepositoryActivityPullRequestUpdateMapping activity, GitHubCommit source)
    {
        target.put(RepositoryActivityCommitMapping.ACTIVITY_ID, activity.getID());
        target.put(RepositoryActivityCommitMapping.DATE, source.getCreatedAt());
        target.put(RepositoryActivityCommitMapping.AUTHOR, source.getCreatedBy());
        target.put(RepositoryActivityCommitMapping.RAW_AUTHOR, source.getCreatedByName());
        target.put(RepositoryActivityCommitMapping.NODE, source.getSha());
        target.put(RepositoryActivityCommitMapping.MESSAGE, source.getMessage());
    }

}
