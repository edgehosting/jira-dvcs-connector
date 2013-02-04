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

import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivitySynchronizer;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestAction;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestLineComment;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessorAggregator;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestCommentService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestLineCommentService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestService;

/**
 * {@link RepositoryActivitySynchronizer} implementation over GitHub repository.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GithubRepositoryActivitySynchronizer implements RepositoryActivitySynchronizer
{

    /**
     * @see #GithubRepositoryActivitySynchronizer(GithubClientProvider, GitHubEventProcessorAggregator, GitHubPullRequestService,
     *      GitHubPullRequestCommentService, GitHubPullRequestLineCommentService)
     */
    private final GithubClientProvider githubClientProvider;

    /**
     * @see #GithubRepositoryActivitySynchronizer(GithubClientProvider, GitHubEventProcessorAggregator, GitHubPullRequestService,
     *      GitHubPullRequestCommentService, GitHubPullRequestLineCommentService)
     */
    private final GitHubEventProcessorAggregator<EventPayload> gitHubEventProcessorAggregator;

    /**
     * @see #GithubRepositoryActivitySynchronizer(GithubClientProvider, GitHubEventProcessorAggregator, GitHubPullRequestService,
     *      GitHubPullRequestCommentService, GitHubPullRequestLineCommentService)
     */
    private final GitHubPullRequestService gitHubPullRequestService;

    /**
     * @see #GithubRepositoryActivitySynchronizer(GithubClientProvider, GitHubEventProcessorAggregator, GitHubPullRequestService,
     *      GitHubPullRequestCommentService, GitHubPullRequestLineCommentService)
     */
    private final GitHubPullRequestCommentService gitHubPullRequestCommentService;

    /**
     * @see #GithubRepositoryActivitySynchronizer(GithubClientProvider, GitHubEventProcessorAggregator, GitHubPullRequestService,
     *      GitHubPullRequestCommentService, GitHubPullRequestLineCommentService)
     */
    private final GitHubPullRequestLineCommentService gitHubPullRequestLineCommentService;

    /**
     * Constructor.
     * 
     * @param githubClientProvider
     *            injected {@link GithubClientProvider} dependency
     * @param gitHubEventProcessorAggregator
     *            injected {@link GitHubEventProcessorAggregator} dependency
     * @param gitHubPullRequestService
     *            injected {@link GitHubPullRequestService} dependency
     * @param gitHubPullRequestCommentService
     *            injected {@link GitHubPullRequestCommentService} dependency
     * @param gitHubPullRequestLineCommentService
     *            injected {@link GitHubPullRequestLineCommentService} dependency
     */
    public GithubRepositoryActivitySynchronizer(//
            GithubClientProvider githubClientProvider, //
            GitHubEventProcessorAggregator<EventPayload> gitHubEventProcessorAggregator, //
            GitHubPullRequestService gitHubPullRequestService, //
            GitHubPullRequestCommentService gitHubPullRequestCommentService, //
            GitHubPullRequestLineCommentService gitHubPullRequestLineCommentService //
    )
    {
        this.githubClientProvider = githubClientProvider;
        this.gitHubEventProcessorAggregator = gitHubEventProcessorAggregator;
        this.gitHubPullRequestService = gitHubPullRequestService;
        this.gitHubPullRequestCommentService = gitHubPullRequestCommentService;
        this.gitHubPullRequestLineCommentService = gitHubPullRequestLineCommentService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void synchronize(Repository forRepository, boolean softSync)
    {
        EventService eventService = githubClientProvider.getEventService(forRepository);

        // gets repository ID
        RepositoryId repositoryId = RepositoryId.create(forRepository.getOrgName(), forRepository.getSlug());

        // goes over events
        Iterator<Collection<Event>> eventsIterator = eventService.pageEvents(repositoryId).iterator();
        while (eventsIterator.hasNext())
        {
            Collection<Event> nextPage = eventsIterator.next();
            for (Event event : nextPage)
            {
                System.out.println("Event: " + event.toString());
                gitHubEventProcessorAggregator.process(forRepository, event);
            }
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
