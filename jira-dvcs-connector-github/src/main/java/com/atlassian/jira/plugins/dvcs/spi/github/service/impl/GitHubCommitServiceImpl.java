package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.CommitUser;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.CommitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.GitHubUtils;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubCommitDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
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
     * @see #GitHubCommitServiceImpl(GitHubCommitDAO, GitHubPullRequestService, GithubClientProvider, RepositoryActivityDao)
     */
    private final GitHubCommitDAO gitHubCommitDAO;

    /**
     * @see #GitHubCommitServiceImpl(GitHubCommitDAO, GitHubPullRequestService, GithubClientProvider, RepositoryActivityDao)
     */
    private final GitHubPullRequestService gitHubPullRequestService;

    /**
     * @see #GitHubCommitServiceImpl(GitHubCommitDAO, GitHubPullRequestService, GithubClientProvider, RepositoryActivityDao)
     */
    private final RepositoryActivityDao repositoryActivityDao;

    /**
     * @see #GitHubCommitServiceImpl(GitHubCommitDAO, GitHubPullRequestService, GithubClientProvider, RepositoryActivityDao)
     */
    private final GithubClientProvider githubClientProvider;

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
     */
    public GitHubCommitServiceImpl(GitHubCommitDAO gitHubCommitDAO, GitHubPullRequestService gitHubPullRequestService,
            @Qualifier("githubClientProvider") GithubClientProvider githubClientProvider, RepositoryActivityDao repositoryActivityDao)
    {
        this.gitHubCommitDAO = gitHubCommitDAO;
        this.gitHubPullRequestService = gitHubPullRequestService;
        this.githubClientProvider = githubClientProvider;
        this.repositoryActivityDao = repositoryActivityDao;
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
    public List<GitHubCommit> getAll(GitHubRepository domain, int first, int count)
    {
        return gitHubCommitDAO.getAll(domain, first, count);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getAllCount(GitHubRepository domain)
    {
        return gitHubCommitDAO.getAllCount(domain);
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
    public void synchronize(Repository domainRepository, GitHubRepository domain)
    {
//        int pageSize = 1024;
//
//        final Map<String, Object> commitMap = new HashMap<String, Object>();
//        RepositoryCommitMapping commit;
//        final Map<String, Object> commitActivityMap = new HashMap<String, Object>();
//
//        for (int i = 0; i < getAllCount(domain); i += pageSize)
//        {
//            for (GitHubCommit gitHubCommit : getAll(domain, i, pageSize))
//            {
//                if (repositoryActivityDao.getCommitByNode(domainRepository, gitHubCommit.getSha()) == null)
//                {
//                    mapToCommit(commitMap, gitHubCommit);
//                    commit = repositoryActivityDao.saveCommit(domainRepository, commitMap);
//                    commitMap.clear();
//
//                    mapToActivity(domainRepository, commitActivityMap, gitHubCommit, commit.getID());
//                    repositoryActivityDao.saveActivity(domainRepository, commitActivityMap);
//                }
//            }
//        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void synchronize(Repository domainRepository, GitHubRepository domain, GitHubPullRequest pullRequest, Progress progress)
    {
//        if (progress.isShouldStop())
//        {
//            return;
//        }
//
//        List<GitHubCommit> allCommits = pullRequest.getCommits();
//        GitHubCommit lastCommit = !allCommits.isEmpty() ? allCommits.get(allCommits.size() - 1) : null;
//
//        // was head changed? is necessary to refresh commits?
//        if (lastCommit == null || !lastCommit.getSha().equals(pullRequest.getHeadSha()))
//        {
//            fetch(domainRepository, domain, pullRequest);
//        }
//
//        synchronizeOpenCommits(pullRequest, domainRepository);
//        synchronizeUpdateCommits(pullRequest, domainRepository);
    }

//    /**
//     * Fetch all new commits for provided pull request.
//     *
//     * @param domainRepository
//     *            for repository
//     * @param domain
//     *            for repository
//     * @param pullRequest
//     *            commits owner
//     */
//    private void fetch(Repository domainRepository, GitHubRepository domain, GitHubPullRequest pullRequest)
//    {
//        pullRequest.getCommits().clear();
//
//        PullRequestService pullRequestService = githubClientProvider.getPullRequestService(domainRepository);
//        try
//        {
//            List<RepositoryCommit> repositoryCommits = pullRequestService.getCommits(
//                    RepositoryId.createFromUrl(domainRepository.getRepositoryUrl()), pullRequest.getNumber());
//            for (RepositoryCommit repositoryCommit : repositoryCommits)
//            {
//                pullRequest.getCommits().add(fetch(domainRepository, domain, pullRequest.getHeadRepository(), repositoryCommit.getSha()));
//            }
//        } catch (IOException e)
//        {
//            throw new RuntimeException(e);
//        }
//
//        gitHubPullRequestService.save(pullRequest);
//    }
//
//    /**
//     * Synchronizes pull request open commits information.
//     *
//     * @param pullRequest
//     *            commits owner
//     * @param domainRepository
//     */
//    private void synchronizeOpenCommits(GitHubPullRequest pullRequest, Repository domainRepository)
//    {
//        RepositoryPullRequestMapping repositoryPullRequest = repositoryActivityDao.findRequestByRemoteId(domainRepository,
//                pullRequest.getGitHubId());
//        List<RepositoryPullRequestUpdateActivityMapping> opened = repositoryActivityDao.getPullRequestActivityByStatus(domainRepository,
//                repositoryPullRequest, RepositoryPullRequestUpdateActivityMapping.Status.OPENED);
//
//        RepositoryPullRequestUpdateActivityMapping openActivity;
//        if (opened.size() == 1)
//        {
//            openActivity = opened.get(0);
//
//        } else if (opened.size() > 1)
//        {
//            LOGGER.error("There are multiple open activities for provided pull request, RepositoryPullRequestID: "
//                    + repositoryPullRequest.getID() + " Founded records: " + opened.size() + " First one will be used!");
//            openActivity = opened.get(0);
//
//        } else
//        {
//            throw new IllegalStateException("Unable to find open activity for provided pull request, RepositoryPullRequest ID: "
//                    + repositoryPullRequest.getID());
//        }
//
//        // SHA to already stored commit
//        Map<String, RepositoryCommitMapping> loadedCommits = new HashMap<String, RepositoryCommitMapping>();
//        for (RepositoryCommitMapping loadedCommit : openActivity.getCommits())
//        {
//            loadedCommits.put(loadedCommit.getNode(), loadedCommit);
//        }
//
//        Iterator<GitHubCommit> commitsIterator = pullRequest.getCommits().iterator();
//        GitHubPullRequestAction openAction = gitHubPullRequestService.getOpenAction(pullRequest);
//
//        //
//        GitHubCommit cursor;
//        final Map<String, Object> commitMap = new HashMap<String, Object>();
//        RepositoryCommitMapping commit;
//        final Map<String, Object> commitActivityMap = new HashMap<String, Object>();
//
//        do
//        {
//            cursor = commitsIterator.next();
//
//            if (!loadedCommits.containsKey(cursor.getSha()))
//            {
//                commit = repositoryActivityDao.getCommitByNode(domainRepository, cursor.getSha());
//                if (commit == null)
//                {
//                    mapToCommit(commitMap, cursor);
//                    commit = repositoryActivityDao.saveCommit(domainRepository, commitMap);
//                    commitMap.clear();
//
//                    mapToActivity(domainRepository, commitActivityMap, cursor, commit.getID());
//                    repositoryActivityDao.saveActivity(domainRepository, commitActivityMap);
//                    commitActivityMap.clear();
//                }
//
//                repositoryActivityDao.linkCommit(domainRepository, openActivity, commit);
//
//            } else
//            {
//                loadedCommits.remove(cursor.getSha());
//
//            }
//
//        } while (!cursor.getSha().equals(openAction.getHeadSha()));
//
//        // unlinks loaded commits, which are not already propagated
//        for (RepositoryCommitMapping toUnlink : loadedCommits.values())
//        {
//            repositoryActivityDao.unlinkCommit(domainRepository, openActivity, toUnlink);
//        }
//    }
//
//    /**
//     * Synchronizes pull request update commits information.
//     *
//     * @param pullRequest
//     *            commits owner
//     * @param domainRepository
//     */
//    private void synchronizeUpdateCommits(GitHubPullRequest pullRequest, Repository domainRepository)
//    {
//        GitHubCommit lastCommit = pullRequest.getCommits().isEmpty() ? null : pullRequest.getCommits().get(
//                pullRequest.getCommits().size() - 1);
//        if (lastCommit == null)
//        {
//            return;
//        }
//
//        // skips open commits
//        GitHubPullRequestAction openAction = gitHubPullRequestService.getOpenAction(pullRequest);
//        Iterator<GitHubCommit> commitsIterator = pullRequest.getCommits().iterator();
//        while (!commitsIterator.next().getSha().equals(openAction.getHeadSha()))
//            ;
//
//        // is there any updated commits?
//        if (!commitsIterator.hasNext())
//        {
//            return;
//        }
//
//        RepositoryPullRequestMapping repositoryPullRequest = repositoryActivityDao.findRequestByRemoteId(domainRepository,
//                pullRequest.getGitHubId());
//        List<RepositoryPullRequestUpdateActivityMapping> updated = repositoryActivityDao.getPullRequestActivityByStatus(domainRepository,
//                repositoryPullRequest, RepositoryPullRequestUpdateActivityMapping.Status.UPDATED);
//
//        RepositoryPullRequestUpdateActivityMapping updateActivity;
//        if (updated.isEmpty())
//        {
//            // it is hack - because this information is not still correct
//            // E.g.: Commit, Push, Commit, Open Pull Request, Push => Updated Activity will be before Opened Activity!
//            // for this case the same date as opened pull request will be used!
//            Date lastUpdatedOn = openAction.getCreatedAt().before(lastCommit.getCreatedAt()) ? lastCommit.getCreatedAt() : openAction
//                    .getCreatedAt();
//
//            Map<String, Object> updateActivityParams = new HashMap<String, Object>();
//            updateActivityParams.put(RepositoryPullRequestActivityMapping.PULL_REQUEST_ID, repositoryPullRequest.getID());
//            updateActivityParams.put(RepositoryPullRequestActivityMapping.REPOSITORY_ID, repositoryPullRequest.getToRepositoryId());
//            updateActivityParams.put(RepositoryPullRequestActivityMapping.ENTITY_TYPE, RepositoryPullRequestUpdateActivityMapping.class);
//            updateActivityParams.put(RepositoryPullRequestActivityMapping.LAST_UPDATED_ON, lastUpdatedOn);
//            updateActivityParams.put(RepositoryPullRequestActivityMapping.AUTHOR, lastCommit.getCreatedBy());
//            updateActivityParams.put(RepositoryPullRequestActivityMapping.RAW_AUTHOR, lastCommit.getCreatedByName());
//            updateActivityParams.put(RepositoryPullRequestUpdateActivityMapping.STATUS,
//                    RepositoryPullRequestUpdateActivityMapping.Status.UPDATED);
//            updateActivity = (RepositoryPullRequestUpdateActivityMapping) repositoryActivityDao.saveActivity(domainRepository,
//                    updateActivityParams);
//
//        } else if (updated.size() == 1)
//        {
//            updateActivity = updated.get(0);
//
//        } else
//        {
//            updateActivity = updated.get(0);
//            LOGGER.error("Currently only one GitHub UPDATED activity per pull request is supported! There was founded: " + updated.size()
//                    + " activities, for Repository Pull Request ID: " + repositoryPullRequest.getID() + " The first one will be used!");
//
//        }
//
//        //
//
//        // SHA to already stored commit
//        Map<String, RepositoryCommitMapping> loadedCommits = new HashMap<String, RepositoryCommitMapping>();
//        for (RepositoryCommitMapping loadedCommit : updateActivity.getCommits())
//        {
//            loadedCommits.put(loadedCommit.getNode(), loadedCommit);
//        }
//
//        //
//        GitHubCommit cursor;
//        final Map<String, Object> commitMap = new HashMap<String, Object>();
//        RepositoryCommitMapping commit;
//        final Map<String, Object> commitActivityMap = new HashMap<String, Object>();
//
//        while (commitsIterator.hasNext())
//        {
//            cursor = commitsIterator.next();
//
//            if (!loadedCommits.containsKey(cursor.getSha()))
//            {
//                commit = repositoryActivityDao.getCommitByNode(domainRepository, cursor.getSha());
//                if (commit == null)
//                {
//                    mapToCommit(commitMap, cursor);
//                    commit = repositoryActivityDao.saveCommit(domainRepository, commitMap);
//                    commitMap.clear();
//
//                    mapToActivity(domainRepository, commitActivityMap, cursor, commit.getID());
//                    repositoryActivityDao.saveActivity(domainRepository, commitActivityMap);
//                    commitActivityMap.clear();
//                }
//
//                repositoryActivityDao.linkCommit(domainRepository, updateActivity, commit);
//
//            } else
//            {
//                loadedCommits.remove(cursor.getSha());
//            }
//        }
//
//        // unlinks loaded commits, which are not already propagated
//        for (RepositoryCommitMapping toUnlink : loadedCommits.values())
//        {
//            repositoryActivityDao.unlinkCommit(domainRepository, updateActivity, toUnlink);
//        }
//
//    }
//
//    /**
//     * Re-maps provided model value into the {@link RepositoryPullRequestUpdateActivityToCommitMapping} AO creation map.
//     *
//     * @param domainRepository
//     *            over which repository
//     * @param target
//     *            AO creation map
//     * @param source
//     *            repository
//     */
//    private void mapToActivity(Repository domainRepository, Map<String, Object> target, GitHubCommit source, int sourceRepositoryId)
//    {
//        target.put(RepositoryCommitActivityMapping.ENTITY_TYPE, RepositoryCommitCommitActivityMapping.class);
//        target.put(RepositoryCommitActivityMapping.COMMIT, sourceRepositoryId);
//        target.put(RepositoryCommitActivityMapping.LAST_UPDATED_ON, source.getCreatedAt());
//        target.put(RepositoryCommitActivityMapping.AUTHOR, source.getCreatedBy());
//        target.put(RepositoryCommitActivityMapping.RAW_AUTHOR, source.getCreatedByName());
//        target.put(RepositoryCommitActivityMapping.REPOSITORY_ID, domainRepository.getId());
//    }
//
//    /**
//     * Re-maps provided model value into the {@link RepositoryCommitMapping} AO creation map.
//     *
//     * @param target
//     *            AO creation map
//     * @param source
//     *            commit
//     */
//    private void mapToCommit(Map<String, Object> target, GitHubCommit source)
//    {
//        target.put(RepositoryCommitMapping.DATE, source.getCreatedAt());
//        target.put(RepositoryCommitMapping.AUTHOR, source.getCreatedBy());
//        target.put(RepositoryCommitMapping.RAW_AUTHOR, source.getCreatedByName());
//        target.put(RepositoryCommitMapping.NODE, source.getSha());
//        target.put(RepositoryCommitMapping.MESSAGE, source.getMessage());
//    }

}
