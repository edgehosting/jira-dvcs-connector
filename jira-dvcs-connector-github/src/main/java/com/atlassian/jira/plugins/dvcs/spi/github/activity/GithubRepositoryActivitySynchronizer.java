package com.atlassian.jira.plugins.dvcs.spi.github.activity;

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import net.java.ao.Query;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.EventPayload;
import org.eclipse.egit.github.core.service.EventService;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestCommentMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestUpdateMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivitySynchronizer;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ColumnNameResolverService;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubEntityMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubEventMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPullRequestActionMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPullRequestCommentMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPullRequestLineCommentMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubUserMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubEvent;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestAction;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestLineComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessorAggregator;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestCommentService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestLineCommentService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestService;
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
     * @param gitHubEventService
     *            injected {@link GitHubRepositoryService} dependency
     * @param gitHubEventService
     *            injected {@link GitHubEventService} dependency
     * @param gitHubPullRequestService
     *            injected {@link GitHubPullRequestService} dependency
     * @param gitHubPullRequestCommentService
     *            injected {@link GitHubPullRequestCommentService} dependency
     * @param gitHubPullRequestLineCommentService
     *            injected {@link GitHubPullRequestLineCommentService} dependency
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
        final GitHubRepository gitHubRepository = gitHubRepositoryService.fetch(forRepository, 0);

        if (!softSync)
        {
            cleanAll(forRepository, gitHubRepository);
        }

        EventService eventService = githubClientProvider.getEventService(forRepository);

        // gets repository ID
        RepositoryId repositoryId = RepositoryId.create(forRepository.getOrgName(), forRepository.getSlug());

        final GitHubEvent savePoint = gitHubEventService.getLastSavePoint(gitHubRepository);

        // goes over events
        Iterator<Collection<Event>> eventsIterator = eventService.pageEvents(repositoryId).iterator();
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
                        gitHubEventProcessorAggregator.process(gitHubRepository, event, forRepository);

                        // saves proceed GitHub event
                        GitHubEvent gitHubEvent = gitHubEventService.getByGitHubId(event.getId());
                        if (gitHubEvent == null)
                        {
                            gitHubEvent = new GitHubEvent();
                        }
                        gitHubEvent.setGitHubId(event.getId());
                        gitHubEvent.setCreatedAt(event.getCreatedAt());
                        gitHubEvent.setRepository(gitHubRepository);
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
                GitHubEvent last = gitHubEventService.getLast(gitHubRepository);
                if (last != null)
                {
                    last.setSavePoint(true);
                    gitHubEventService.save(last);
                }

                return null;
            }

        });

        repositoryActivityDao.removeAll(forRepository);

        // dump
        SortedMap<Date, String> dump = new TreeMap<Date, String>(new Comparator<Date>()
        {

            @Override
            public int compare(Date o1, Date o2)
            {
                return o1.compareTo(o2);
            }

        });

        for (GitHubPullRequest gitHubPullRequest : gitHubPullRequestService.getAll())
        {
            RepositoryPullRequestMapping repositoryPullRequest = getRepositoryPullRequest(gitHubPullRequest);

            for (GitHubPullRequestAction gitHubPullRequestAction : gitHubPullRequest.getActions())
            {
                Map<String, Object> activity = new HashMap<String, Object>();
                map(activity, gitHubPullRequest, repositoryPullRequest);
                map(activity, gitHubPullRequestAction);
                repositoryActivityDao.saveActivity(activity);

                dump.put(gitHubPullRequestAction.getCreatedAt(), //
                        gitHubPullRequestAction.getCreatedBy().getName() // name
                                + " " + gitHubPullRequestAction.getAction().name() // action
                                + " the pull request " + gitHubPullRequest.getTitle() // title
                                + " on " + gitHubPullRequestAction.getCreatedAt() // date
                );
            }
        }

        for (GitHubPullRequestLineComment gitHubPullRequestLineComment : gitHubPullRequestLineCommentService.getAll())
        {
            dump.put(gitHubPullRequestLineComment.getCreatedAt(), //
                    gitHubPullRequestLineComment.getCreatedBy().getName() // name
                            + " left a comment " + gitHubPullRequestLineComment.getPath() + ":" + gitHubPullRequestLineComment.getLine() // position
                            + " on a pull request " + gitHubPullRequestLineComment.getPullRequest().getTitle() // title
                            + " on " + gitHubPullRequestLineComment.getCreatedAt() // date
                            + "(" + gitHubPullRequestLineComment.getText() + ")" //
            );
        }

        for (GitHubPullRequestComment gitHubPullRequestComment : gitHubPullRequestCommentService.getAll())
        {
            RepositoryPullRequestMapping repositoryPullRequest = getRepositoryPullRequest(gitHubPullRequestComment.getPullRequest());

            Map<String, Object> activity = new HashMap<String, Object>();
            map(activity, gitHubPullRequestComment.getPullRequest(), repositoryPullRequest);
            map(activity, gitHubPullRequestComment);
            repositoryActivityDao.saveActivity(activity);

            dump.put(gitHubPullRequestComment.getCreatedAt(), //
                    gitHubPullRequestComment.getCreatedBy().getName() // name
                            + " left a comment " //
                            + " on a pull request " + gitHubPullRequestComment.getPullRequest().getTitle() // title
                            + " on " + gitHubPullRequestComment.getCreatedAt() // date
                            + " (" + gitHubPullRequestComment.getText() + ")" //
            );
        }

        System.out.println("==== Begin: Activity dump ====");
        for (Entry<Date, String> entry : dump.entrySet())
        {
            System.out.println(entry.getValue());
        }
        System.out.println("==== End of: Activity dump ====");
    }

    RepositoryPullRequestMapping getRepositoryPullRequest(GitHubPullRequest source)
    {
        RepositoryPullRequestMapping result = repositoryActivityDao.findRequestById(source.getId(), source.getBaseRepository().getName());
        if (result != null)
        {
            return result;
        }

        Set<String> issueKeys = IssueKeyExtractor.extractIssueKeys(source.getTitle());

        Map<String, Object> params = new HashMap<String, Object>();
        params.put(RepositoryPullRequestMapping.LOCAL_ID, source.getId());
        params.put(RepositoryPullRequestMapping.PULL_REQUEST_URL, source.getUrl());
        params.put(RepositoryPullRequestMapping.PULL_REQUEST_NAME, source.getTitle());
        params.put(RepositoryPullRequestMapping.FOUND_ISSUE_KEY, !issueKeys.isEmpty());
        params.put(RepositoryPullRequestMapping.TO_REPO_SLUG, source.getBaseRepository().getName());
        result = repositoryActivityDao.savePullRequest(params, issueKeys);

        return result;
    }

    private void map(Map<String, Object> activity, GitHubPullRequest pullRequest, RepositoryPullRequestMapping repositoryPullRequest)
    {
        activity.put(RepositoryActivityPullRequestMapping.PULL_REQUEST_ID, repositoryPullRequest.getID());
        activity.put(RepositoryActivityPullRequestMapping.REPO_SLUG, pullRequest.getBaseRepository().getName());
    }

    private void map(Map<String, Object> activity, GitHubPullRequestAction action)
    {
        activity.put(RepositoryActivityPullRequestMapping.ENTITY_TYPE, RepositoryActivityPullRequestUpdateMapping.class);
        activity.put(RepositoryActivityPullRequestMapping.LAST_UPDATED_ON, action.getCreatedAt());
        activity.put(RepositoryActivityPullRequestMapping.INITIATOR_USERNAME, action.getCreatedBy().getLogin());
        activity.put(RepositoryActivityPullRequestUpdateMapping.STATUS, action.getAction().name());
    }

    private void map(Map<String, Object> activity, GitHubPullRequestComment comment)
    {
        activity.put(RepositoryActivityPullRequestMapping.ENTITY_TYPE, RepositoryActivityPullRequestCommentMapping.class);
        activity.put(RepositoryActivityPullRequestMapping.LAST_UPDATED_ON, comment.getCreatedAt());
        activity.put(RepositoryActivityPullRequestMapping.INITIATOR_USERNAME, comment.getCreatedBy().getLogin());
        activity.put(RepositoryActivityPullRequestCommentMapping.MESSAGE, comment.getText());
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
                GitHubPullRequestMapping.class, GitHubUserMapping.class, GitHubEventMapping.class };

        for (Class<? extends GitHubEntityMapping> entityToClean : entitiesForClean)
        {
            ActiveObjectsUtils.delete(
                    activeObjects,
                    entityToClean,
                    Query.select().where(columnNameResolverService.column(gitHubEntityMappingDescription.getRepository()) + " = ? ",
                            gitHubRepository.getId()));
        }

    }

}
