package com.atlassian.jira.plugins.dvcs.spi.github.activity;

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.EventPayload;
import org.eclipse.egit.github.core.service.EventService;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivitySynchronizer;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubEvent;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestAction;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestLineComment;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessorAggregator;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestCommentService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestLineCommentService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestService;
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
     * @see #GithubRepositoryActivitySynchronizer(ActiveObjects, GithubClientProvider, GitHubEventProcessorAggregator, GitHubEventService,
     *      GitHubPullRequestService, GitHubPullRequestCommentService, GitHubPullRequestLineCommentService)
     */
    private final ActiveObjects activeObjects;

    /**
     * @see #GithubRepositoryActivitySynchronizer(ActiveObjects, GithubClientProvider, GitHubEventProcessorAggregator, GitHubEventService,
     *      GitHubPullRequestService, GitHubPullRequestCommentService, GitHubPullRequestLineCommentService)
     */
    private final GithubClientProvider githubClientProvider;

    /**
     * @see #GithubRepositoryActivitySynchronizer(ActiveObjects, GithubClientProvider, GitHubEventProcessorAggregator, GitHubEventService,
     *      GitHubPullRequestService, GitHubPullRequestCommentService, GitHubPullRequestLineCommentService)
     */
    private final GitHubEventProcessorAggregator<EventPayload> gitHubEventProcessorAggregator;

    /**
     * @see #GithubRepositoryActivitySynchronizer(ActiveObjects, GithubClientProvider, GitHubEventProcessorAggregator, GitHubEventService,
     *      GitHubPullRequestService, GitHubPullRequestCommentService, GitHubPullRequestLineCommentService)
     */
    private final GitHubEventService gitHubEventService;

    /**
     * @see #GithubRepositoryActivitySynchronizer(ActiveObjects, GithubClientProvider, GitHubEventProcessorAggregator, GitHubEventService,
     *      GitHubPullRequestService, GitHubPullRequestCommentService, GitHubPullRequestLineCommentService)
     */
    private final GitHubPullRequestService gitHubPullRequestService;

    /**
     * @see #GithubRepositoryActivitySynchronizer(ActiveObjects, GithubClientProvider, GitHubEventProcessorAggregator, GitHubEventService,
     *      GitHubPullRequestService, GitHubPullRequestCommentService, GitHubPullRequestLineCommentService)
     */
    private final GitHubPullRequestCommentService gitHubPullRequestCommentService;

    /**
     * @see #GithubRepositoryActivitySynchronizer(ActiveObjects, GithubClientProvider, GitHubEventProcessorAggregator, GitHubEventService,
     *      GitHubPullRequestService, GitHubPullRequestCommentService, GitHubPullRequestLineCommentService)
     */
    private final GitHubPullRequestLineCommentService gitHubPullRequestLineCommentService;

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
     *            injected {@link GitHubEventService} dependency
     * @param gitHubPullRequestService
     *            injected {@link GitHubPullRequestService} dependency
     * @param gitHubPullRequestCommentService
     *            injected {@link GitHubPullRequestCommentService} dependency
     * @param gitHubPullRequestLineCommentService
     *            injected {@link GitHubPullRequestLineCommentService} dependency
     */
    public GithubRepositoryActivitySynchronizer(//
            ActiveObjects activeObjects, //
            GithubClientProvider githubClientProvider, //
            GitHubEventProcessorAggregator<EventPayload> gitHubEventProcessorAggregator, //
            GitHubEventService gitHubEventService, //
            GitHubPullRequestService gitHubPullRequestService, //
            GitHubPullRequestCommentService gitHubPullRequestCommentService, //
            GitHubPullRequestLineCommentService gitHubPullRequestLineCommentService //
    )
    {
        this.activeObjects = activeObjects;
        this.githubClientProvider = githubClientProvider;
        this.gitHubEventProcessorAggregator = gitHubEventProcessorAggregator;
        this.gitHubEventService = gitHubEventService;
        this.gitHubPullRequestService = gitHubPullRequestService;
        this.gitHubPullRequestCommentService = gitHubPullRequestCommentService;
        this.gitHubPullRequestLineCommentService = gitHubPullRequestLineCommentService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void synchronize(final Repository forRepository, boolean softSync)
    {
        EventService eventService = githubClientProvider.getEventService(forRepository);

        // gets repository ID
        RepositoryId repositoryId = RepositoryId.create(forRepository.getOrgName(), forRepository.getSlug());

        final GitHubEvent savePoint = gitHubEventService.getLastSavePoint();

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
                        gitHubEventProcessorAggregator.process(forRepository, event);

                        // saves proceed GitHub event
                        GitHubEvent gitHubEvent = new GitHubEvent();
                        gitHubEvent.setGitHubId(event.getId());
                        gitHubEvent.setCreatedAt(event.getCreatedAt());
                        gitHubEventService.save(gitHubEvent);
                    }

                    return null;
                }
            });

        }

        // marks new save point
        GitHubEvent last = gitHubEventService.getLast();
        if (last != null)
        {
            last.setSavePoint(true);
            gitHubEventService.save(last);
        }

        // dump
        String issueKey = null;
        SortedMap<Date, String> dump = new TreeMap<Date, String>(new Comparator<Date>()
        {

            @Override
            public int compare(Date o1, Date o2)
            {
                return o1.compareTo(o2);
            }

        });

        for (GitHubPullRequest gitHubPullRequest : gitHubPullRequestService.getGitHubPullRequest(issueKey))
        {
            for (GitHubPullRequestAction gitHubPullRequestAction : gitHubPullRequest.getActions())
            {
                dump.put(gitHubPullRequestAction.getCreatedAt(), //
                        gitHubPullRequestAction.getCreatedBy().getName() // name
                                + " " + gitHubPullRequestAction.getAction().name() // action
                                + " the pull request " + gitHubPullRequest.getTitle() // title
                                + " on " + gitHubPullRequestAction.getCreatedAt() // date
                );
            }
        }

        for (GitHubPullRequestLineComment gitHubPullRequestLineComment : gitHubPullRequestLineCommentService.getByIssueKey(issueKey))
        {
            dump.put(gitHubPullRequestLineComment.getCreatedAt(), //
                    gitHubPullRequestLineComment.getCreatedBy().getName() // name
                            + " left a comment " + gitHubPullRequestLineComment.getPath() + ":" + gitHubPullRequestLineComment.getLine() // position
                            + " on a pull request " + gitHubPullRequestLineComment.getPullRequest().getTitle() // title
                            + " on " + gitHubPullRequestLineComment.getCreatedAt() // date
                            + "(" + gitHubPullRequestLineComment.getText() + ")" //
            );
        }

        for (GitHubPullRequestComment gitHubPullRequestComment : gitHubPullRequestCommentService.getByIssueKey(issueKey))
        {
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
}
