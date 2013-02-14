package com.atlassian.jira.plugins.dvcs.spi.github.activity;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.java.ao.Query;

import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.EventPayload;
import org.eclipse.egit.github.core.event.PushPayload;
import org.eclipse.egit.github.core.service.EventService;
import org.eclipse.egit.github.core.service.PullRequestService;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityCommitMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestCommentMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestUpdateMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivitySynchronizer;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ColumnNameResolverService;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubCommitMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubEntityMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubEventMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPullRequestActionMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPullRequestCommentMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPullRequestLineCommentMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPushCommitMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPushMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubUserMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubEvent;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestAction;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPush;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessorAggregator;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestCommentService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestLineCommentService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPushService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubRepositoryService;
import com.atlassian.jira.plugins.dvcs.util.ActiveObjectsUtils;
import com.atlassian.jira.plugins.dvcs.util.IssueKeyExtractor;
import com.atlassian.sal.api.transaction.TransactionCallback;

/**
 * {@link RepositoryActivitySynchronizer} implementation over GitHub repository.
 * 
 * @author Stanislav Dvorscak
 * 
 */
// FIXME<stanislav-dvorscak>: remove system out
public class GithubRepositoryActivitySynchronizer implements RepositoryActivitySynchronizer
{

    /**
     * @see #GithubRepositoryActivitySynchronizer(ActiveObjects, GithubClientProvider, GitHubEventProcessorAggregator,
     *      GitHubRepositoryService, GitHubEventService, GitHubPullRequestService, GitHubPullRequestCommentService,
     *      GitHubPullRequestLineCommentService, RepositoryActivityDao, ColumnNameResolverService)
     */
    private final ActiveObjects activeObjects;

    /**
     * @see #GithubRepositoryActivitySynchronizer(ActiveObjects, GithubClientProvider, GitHubEventProcessorAggregator,
     *      GitHubRepositoryService, GitHubEventService, GitHubPullRequestService, GitHubPullRequestCommentService,
     *      GitHubPullRequestLineCommentService, RepositoryActivityDao, ColumnNameResolverService)
     */
    private final GithubClientProvider githubClientProvider;

    /**
     * @see #GithubRepositoryActivitySynchronizer(ActiveObjects, GithubClientProvider, GitHubEventProcessorAggregator,
     *      GitHubRepositoryService, GitHubEventService, GitHubPullRequestService, GitHubPullRequestCommentService,
     *      GitHubPullRequestLineCommentService, RepositoryActivityDao, ColumnNameResolverService)
     */
    private final GitHubEventProcessorAggregator<EventPayload> gitHubEventProcessorAggregator;

    /**
     * @see #GithubRepositoryActivitySynchronizer(ActiveObjects, GithubClientProvider, GitHubEventProcessorAggregator,
     *      GitHubRepositoryService, GitHubEventService, GitHubPullRequestService, GitHubPullRequestCommentService,
     *      GitHubPullRequestLineCommentService, RepositoryActivityDao, ColumnNameResolverService)
     */
    private final GitHubRepositoryService gitHubRepositoryService;

    /**
     * @see #GithubRepositoryActivitySynchronizer(ActiveObjects, GithubClientProvider, GitHubEventProcessorAggregator,
     *      GitHubRepositoryService, GitHubEventService, GitHubPullRequestService, GitHubPullRequestCommentService,
     *      GitHubPullRequestLineCommentService, RepositoryActivityDao, ColumnNameResolverService)
     */
    private final GitHubEventService gitHubEventService;

    /**
     * @see #GithubRepositoryActivitySynchronizer(ActiveObjects, GithubClientProvider, GitHubEventProcessorAggregator,
     *      GitHubRepositoryService, GitHubEventService, GitHubPullRequestService, GitHubPullRequestCommentService,
     *      GitHubPullRequestLineCommentService, RepositoryActivityDao, ColumnNameResolverService)
     */
    private final GitHubPullRequestService gitHubPullRequestService;

    /**
     * @see #GithubRepositoryActivitySynchronizer(ActiveObjects, GithubClientProvider, GitHubEventProcessorAggregator,
     *      GitHubRepositoryService, GitHubEventService, GitHubPullRequestService, GitHubPullRequestCommentService,
     *      GitHubPullRequestLineCommentService, RepositoryActivityDao, ColumnNameResolverService)
     */
    private final GitHubPullRequestCommentService gitHubPullRequestCommentService;

    /**
     * @see #GithubRepositoryActivitySynchronizer(ActiveObjects, GithubClientProvider, GitHubEventProcessorAggregator,
     *      GitHubRepositoryService, GitHubEventService, GitHubPullRequestService, GitHubPullRequestCommentService,
     *      GitHubPullRequestLineCommentService, RepositoryActivityDao, ColumnNameResolverService)
     */
    private final GitHubPullRequestLineCommentService gitHubPullRequestLineCommentService;

    /**
     * 
     */
    private final GitHubPushService gitHubPushService;

    /**
     * @see #GithubRepositoryActivitySynchronizer(ActiveObjects, GithubClientProvider, GitHubEventProcessorAggregator,
     *      GitHubRepositoryService, GitHubEventService, GitHubPullRequestService, GitHubPullRequestCommentService,
     *      GitHubPullRequestLineCommentService, RepositoryActivityDao, ColumnNameResolverService)
     */
    private final RepositoryActivityDao repositoryActivityDao;

    /**
     * @see #GithubRepositoryActivitySynchronizer(ActiveObjects, GithubClientProvider, GitHubEventProcessorAggregator,
     *      GitHubRepositoryService, GitHubEventService, GitHubPullRequestService, GitHubPullRequestCommentService,
     *      GitHubPullRequestLineCommentService, RepositoryActivityDao, ColumnNameResolverService)
     */
    private final ColumnNameResolverService columnNameResolverService;

    /**
     * {@link ColumnNameResolverService#desc(Class)} of the {@link GitHubEntityMapping}.
     */
    private final GitHubEntityMapping gitHubEntityMappingDescription;

    /**
     * Constructor.
     * 
     * @param activeObjects
     *            injected {@link ActiveObjects} dependency
     * @param githubClientProvider
     *            injected {@link GithubClientProvider} dependency
     * @param gitHubEventProcessorAggregator
     *            injected {@link GitHubEventProcessorAggregator} dependency
     * @param gitHubRepositoryService
     *            injected {@link GitHubRepositoryService} dependency
     * @param gitHubEventService
     *            injected {@link GitHubEventService} dependency
     * @param gitHubPullRequestService
     *            injected {@link GitHubPullRequestService} dependency
     * @param gitHubPullRequestCommentService
     *            injected {@link GitHubPullRequestCommentService} dependency
     * @param gitHubPullRequestLineCommentService
     *            injected {@link GitHubPullRequestLineCommentService} dependency
     * @param gitHubPushService
     *            injected {@link GitHubPushService} dependency
     * @param repositoryActivityDao
     *            injected {@link RepositoryActivityDao} dependency
     * @param columnNameResolverService
     *            injected {@link ColumnNameResolverService} dependency
     */
    public GithubRepositoryActivitySynchronizer(//
            ActiveObjects activeObjects, //
            GithubClientProvider githubClientProvider, //
            GitHubEventProcessorAggregator<EventPayload> gitHubEventProcessorAggregator, //
            GitHubRepositoryService gitHubRepositoryService, //
            GitHubEventService gitHubEventService, //
            GitHubPullRequestService gitHubPullRequestService, //
            GitHubPullRequestCommentService gitHubPullRequestCommentService, //
            GitHubPullRequestLineCommentService gitHubPullRequestLineCommentService, //
            GitHubPushService gitHubPushService, //
            RepositoryActivityDao repositoryActivityDao, //
            ColumnNameResolverService columnNameResolverService //
    )
    {
        this.activeObjects = activeObjects;
        this.githubClientProvider = githubClientProvider;
        this.gitHubEventProcessorAggregator = gitHubEventProcessorAggregator;
        this.gitHubRepositoryService = gitHubRepositoryService;
        this.gitHubEventService = gitHubEventService;
        this.gitHubPullRequestService = gitHubPullRequestService;
        this.gitHubPullRequestCommentService = gitHubPullRequestCommentService;
        this.gitHubPullRequestLineCommentService = gitHubPullRequestLineCommentService;
        this.gitHubPushService = gitHubPushService;
        this.repositoryActivityDao = repositoryActivityDao;

        this.columnNameResolverService = columnNameResolverService;
        gitHubEntityMappingDescription = columnNameResolverService.desc(GitHubEntityMapping.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void synchronize(final Repository forRepository, boolean softSync)
    {
        final GitHubRepository domain = gitHubRepositoryService
                .fetch(forRepository, forRepository.getOrgName(), forRepository.getName(), 0);

        if (!softSync)
        {
            cleanAll(forRepository, domain);
        }

        EventService eventService = githubClientProvider.getEventService(forRepository);

        // gets repository ID
        RepositoryId forRepositoryId = RepositoryId.create(forRepository.getOrgName(), forRepository.getSlug());

        final GitHubEvent savePoint = gitHubEventService.getLastSavePoint(domain);

        // goes over events
        Iterator<Collection<Event>> eventsIterator = eventService.pageEvents(forRepositoryId).iterator();
        while (eventsIterator.hasNext())
        {
            final Collection<Event> nextPage = eventsIterator.next();

            // process whole page in one transaction
            activeObjects.executeInTransaction(new TransactionCallback<Void>()
            {

                @Override
                public Void doInTransaction()
                {
                    for (Event event : nextPage)
                    {
                        // before, not before or equals - there can exists several events with the same timestamp, but it does not mean that
                        // all of them was already proceed
                        if (savePoint != null && event.getCreatedAt().before(savePoint.getCreatedAt()))
                        {
                            // was all previous records proceed?
                            break;

                        } else if (gitHubEventService.getByGitHubId(event.getId()) != null)
                        {
                            // was already proceed? maybe partial synchronization, and there can exist remaining events for processing
                            continue;

                        }

                        System.out.println("Event: " + event.toString());
                        gitHubEventProcessorAggregator.process(forRepository, domain, event);

                        // saves proceed GitHub event
                        GitHubEvent gitHubEvent = gitHubEventService.getByGitHubId(event.getId());
                        if (gitHubEvent == null)
                        {
                            gitHubEvent = new GitHubEvent();
                        }
                        gitHubEvent.setGitHubId(event.getId());
                        gitHubEvent.setCreatedAt(event.getCreatedAt());
                        gitHubEvent.setDomain(domain);
                        gitHubEventService.save(gitHubEvent);
                    }

                    return null;
                }
            });

        }

        // marks new save point
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                GitHubEvent last = gitHubEventService.getLast(domain);
                if (last != null)
                {
                    last.setSavePoint(true);
                    gitHubEventService.save(last);
                }

                return null;
            }

        });

        for (GitHubPullRequest gitHubPullRequest : gitHubPullRequestService.getByRepository(domain))
        {
            downloadPullRequestPushes(forRepository, domain, gitHubPullRequest);
        }

        // clean & rebuild activities
        repositoryActivityDao.removeAll(forRepository);

        for (GitHubPullRequest gitHubPullRequest : gitHubPullRequestService.getByRepository(domain))
        {
            RepositoryPullRequestMapping repositoryPullRequest = getRepositoryPullRequest(gitHubPullRequest, forRepository);

            for (GitHubPullRequestAction gitHubPullRequestAction : gitHubPullRequest.getActions())
            {
                Map<String, Object> activity = new HashMap<String, Object>();
                map(activity, repositoryPullRequest);
                map(activity, gitHubPullRequestAction);

                // skip unsupported status
                if (activity.get(RepositoryActivityPullRequestUpdateMapping.STATUS) == null)
                {
                    continue;
                }

                RepositoryActivityPullRequestMapping createdActivity = repositoryActivityDao.saveActivity(activity);

                if (GitHubPullRequestAction.Action.OPENED.equals(gitHubPullRequestAction.getAction()))
                {
                    updateCommits(forRepository, forRepositoryId, repositoryPullRequest, gitHubPullRequest,
                            (RepositoryActivityPullRequestUpdateMapping) createdActivity);
                }
            }
        }

        for (GitHubPullRequestComment gitHubPullRequestComment : gitHubPullRequestCommentService.getByRepository(domain))
        {
            RepositoryPullRequestMapping repositoryPullRequest = getRepositoryPullRequest(gitHubPullRequestComment.getPullRequest(),
                    forRepository);

            Map<String, Object> activity = new HashMap<String, Object>();
            map(activity, repositoryPullRequest);
            map(activity, gitHubPullRequestComment);
            repositoryActivityDao.saveActivity(activity);
        }

        // FIXME
        Query query = Query.select().where(RepositoryActivityPullRequestUpdateMapping.REPOSITORY_ID + " = ? ", forRepository.getId());
        query.order(RepositoryActivityPullRequestUpdateMapping.LAST_UPDATED_ON);
        for (RepositoryActivityPullRequestUpdateMapping activity : activeObjects.find(RepositoryActivityPullRequestUpdateMapping.class,
                query))
        {
            System.out.println(activity.getPullRequestId() + ":" + activity.getStatus());
            for (RepositoryActivityCommitMapping commit : activity.getCommits())
            {
                System.out.println("\t" + commit.getNode() + ":" + commit.getMessage());
            }
        }
    }

    private void downloadPullRequestPushes(Repository domainRepository, GitHubRepository domain, GitHubPullRequest pullRequest)
    {
        EventService eventService = githubClientProvider.getEventService(domainRepository);
        PageIterator<Event> pageEvents = eventService.pageEvents(RepositoryId.createFromUrl(pullRequest.getHeadRepository().getUrl()));
        while (pageEvents.hasNext())
        {
            for (Event event : pageEvents.next())
            {
                if (event.getPayload() instanceof PushPayload)
                {
                    gitHubEventProcessorAggregator.process(domainRepository, domain, event);
                }
            }
        }
    }

    private void updateCommits(Repository forRepository, IRepositoryIdProvider forRepositoryId,
            RepositoryPullRequestMapping repositoryPullRequest, GitHubPullRequest pullRequest,
            RepositoryActivityPullRequestUpdateMapping pullRequestCreatedUpdateActivity)
    {
        String firstHeadSha;
        String initialHeadSha = null;
        String currentHeadSha;

        for (GitHubPullRequestAction action : pullRequest.getActions())
        {
            if (GitHubPullRequestAction.Action.OPENED.equals(action.getAction()))
            {
                initialHeadSha = action.getHeadSha();

                break;
            }
        }

        PullRequestService pullRequestService = githubClientProvider.getPullRequestService(forRepository);
        List<RepositoryCommit> commits;
        try
        {
            commits = pullRequestService.getCommits(forRepositoryId, pullRequest.getNumber());
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        firstHeadSha = commits.get(0).getSha();
        currentHeadSha = commits.get(commits.size() - 1).getSha();

        GitHubPush pushCursor;
        Map<String, Object> commitActivity = new HashMap<String, Object>();

        // initial commits
        {
            Iterator<GitHubPush> initialPushes = gitHubPushService.getByBetween(pullRequest.getHeadRepository(), firstHeadSha,
                    initialHeadSha).iterator();
            do
            {
                pushCursor = initialPushes.next();
                for (GitHubCommit commit : pushCursor.getCommits())
                {
                    map(commitActivity, commit, pullRequestCreatedUpdateActivity);
                    activeObjects.create(RepositoryActivityCommitMapping.class, commitActivity);
                    commitActivity.clear();
                }
            } while (initialPushes.hasNext());
        }

        // update commits
        {
            Iterator<GitHubPush> updatePushes = gitHubPushService.getByBetween(pullRequest.getHeadRepository(), pushCursor.getHead(),
                    currentHeadSha).iterator();
            while (updatePushes.hasNext())
            {
                pushCursor = updatePushes.next();
                Map<String, Object> commitsUpdateActivity = new HashMap<String, Object>();
                map(commitsUpdateActivity, repositoryPullRequest);
                map(commitsUpdateActivity, pushCursor);
                RepositoryActivityPullRequestUpdateMapping commitsUpdateActivityMapping = (RepositoryActivityPullRequestUpdateMapping) repositoryActivityDao
                        .saveActivity(commitsUpdateActivity);
                for (GitHubCommit commit : pushCursor.getCommits())
                {
                    map(commitActivity, commit, commitsUpdateActivityMapping);
                    repositoryActivityDao.saveCommit(commitActivity);
                    commitActivity.clear();
                }
            }
        }
    }

    RepositoryPullRequestMapping getRepositoryPullRequest(GitHubPullRequest source, Repository forRepository)
    {
        RepositoryPullRequestMapping result = repositoryActivityDao.findRequestById(source.getId(), forRepository.getId());

        if (result != null)
        {
            return result;
        }

        Set<String> issueKeys = IssueKeyExtractor.extractIssueKeys(source.getTitle(), source.getText());

        Map<String, Object> params = new HashMap<String, Object>();
        params.put(RepositoryPullRequestMapping.LOCAL_ID, source.getId());
        params.put(RepositoryPullRequestMapping.URL, source.getUrl());
        params.put(RepositoryPullRequestMapping.NAME, source.getTitle());
        params.put(RepositoryPullRequestMapping.DESCRIPTION, source.getText());
        params.put(RepositoryPullRequestMapping.FOUND_ISSUE_KEY, !issueKeys.isEmpty());
        params.put(RepositoryPullRequestMapping.TO_REPO_ID, forRepository.getId());
        result = repositoryActivityDao.savePullRequest(params, issueKeys);

        return result;
    }

    private void map(Map<String, Object> activity, RepositoryPullRequestMapping repositoryPullRequest)
    {
        activity.put(RepositoryActivityPullRequestMapping.PULL_REQUEST_ID, repositoryPullRequest.getID());
        activity.put(RepositoryActivityPullRequestMapping.REPOSITORY_ID, repositoryPullRequest.getToRepositoryId());
    }

    private void map(Map<String, Object> activity, GitHubPullRequestAction action)
    {
        activity.put(RepositoryActivityPullRequestMapping.ENTITY_TYPE, RepositoryActivityPullRequestUpdateMapping.class);
        activity.put(RepositoryActivityPullRequestMapping.LAST_UPDATED_ON, action.getCreatedAt());
        activity.put(RepositoryActivityPullRequestMapping.AUTHOR, action.getCreatedBy().getLogin());

        RepositoryActivityPullRequestUpdateMapping.Status status = null;
        if (GitHubPullRequestAction.Action.OPENED.equals(action.getAction()))
        {
            status = RepositoryActivityPullRequestUpdateMapping.Status.OPENED;

        } else if (GitHubPullRequestAction.Action.MERGED.equals(action.getAction()))
        {
            status = RepositoryActivityPullRequestUpdateMapping.Status.MERGED;

        } else if (GitHubPullRequestAction.Action.CLOSED.equals(action.getAction()))
        {
            status = RepositoryActivityPullRequestUpdateMapping.Status.DECLINED;

        } else if (GitHubPullRequestAction.Action.REOPENED.equals(action.getAction()))
        {
            status = RepositoryActivityPullRequestUpdateMapping.Status.REOPENED;

        }
        activity.put(RepositoryActivityPullRequestUpdateMapping.STATUS, status);
    }

    private void map(Map<String, Object> activity, GitHubPush push)
    {
        activity.put(RepositoryActivityPullRequestMapping.ENTITY_TYPE, RepositoryActivityPullRequestUpdateMapping.class);
        activity.put(RepositoryActivityPullRequestMapping.LAST_UPDATED_ON, push.getCreatedAt());
        activity.put(RepositoryActivityPullRequestMapping.AUTHOR, push.getCreatedBy().getLogin());
        activity.put(RepositoryActivityPullRequestUpdateMapping.STATUS, RepositoryActivityPullRequestUpdateMapping.Status.UPDATED);
    }

    private void map(Map<String, Object> activity, GitHubPullRequestComment comment)
    {
        activity.put(RepositoryActivityPullRequestMapping.ENTITY_TYPE, RepositoryActivityPullRequestCommentMapping.class);
        activity.put(RepositoryActivityPullRequestMapping.LAST_UPDATED_ON, comment.getCreatedAt());
        activity.put(RepositoryActivityPullRequestMapping.AUTHOR, comment.getCreatedBy().getLogin());
        activity.put(RepositoryActivityPullRequestCommentMapping.MESSAGE, comment.getText());
    }

    private void map(Map<String, Object> commitActivity, GitHubCommit commit, RepositoryActivityPullRequestUpdateMapping activity)
    {
        commitActivity.put(RepositoryActivityCommitMapping.ACTIVITY_ID, activity.getID());
        commitActivity.put(RepositoryActivityCommitMapping.AUTHOR, commit.getCreatedBy());
        commitActivity.put(RepositoryActivityCommitMapping.RAW_AUTHOR, commit.getCreatedByName());
        commitActivity.put(RepositoryActivityCommitMapping.AUTHOR_AVATAR_URL, commit.getCreatedByAvatarUrl());
        commitActivity.put(RepositoryActivityCommitMapping.NODE, commit.getSha());
        commitActivity.put(RepositoryActivityCommitMapping.MESSAGE, commit.getMessage());
    }

    /**
     * Cleans all tables.
     * 
     * @param forRepository
     * @param gitHubRepository
     */
    private void cleanAll(Repository forRepository, GitHubRepository gitHubRepository)
    {
        repositoryActivityDao.removeAll(forRepository);

        @SuppressWarnings("unchecked")
        Class<? extends GitHubEntityMapping>[] entitiesForClean = (Class<? extends GitHubEntityMapping>[]) new Class[] {
                GitHubPullRequestLineCommentMapping.class, GitHubPullRequestCommentMapping.class, GitHubPullRequestActionMapping.class,
                GitHubPullRequestMapping.class, GitHubPushCommitMapping.class, GitHubCommitMapping.class, GitHubPushMapping.class,
                GitHubUserMapping.class, GitHubEventMapping.class };

        for (Class<? extends GitHubEntityMapping> entityToClean : entitiesForClean)
        {
            ActiveObjectsUtils.delete(
                    activeObjects,
                    entityToClean,
                    Query.select().where(columnNameResolverService.column(gitHubEntityMappingDescription.getDomain()) + " = ? ",
                            gitHubRepository.getId()));
        }

    }

}
