package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

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
import org.eclipse.egit.github.core.service.GitHubService;

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
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubRepositoryService;

/**
 * An {@link GitHubService} implementation.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubRepositoryServiceImpl implements GitHubRepositoryService
{

    /**
     * @see #GitHubRepositoryServiceImpl(GithubClientProvider, GitHubEventProcessorAggregator, GitHubPullRequestService,
     *      GitHubPullRequestLineCommentService)
     */
    private final GithubClientProvider githubClientProvider;

    /**
     * @see #GitHubRepositoryServiceImpl(GithubClientProvider, GitHubEventProcessorAggregator, GitHubPullRequestService,
     *      GitHubPullRequestLineCommentService)
     */
    private final GitHubEventProcessorAggregator<EventPayload> eventProcessorAggregator;

    /**
     * @see #GitHubRepositoryServiceImpl(GithubClientProvider, GitHubEventProcessorAggregator, GitHubPullRequestService,
     *      GitHubPullRequestLineCommentService)
     */
    private final GitHubPullRequestService gitHubPullRequestService;

    /**
     * @see #GitHubRepositoryServiceImpl(GithubClientProvider, GitHubEventProcessorAggregator, GitHubPullRequestService,
     *      GitHubPullRequestLineCommentService)
     */
    private final GitHubPullRequestCommentService gitHubPullRequestCommentService;

    /**
     * @see #GitHubRepositoryServiceImpl(GithubClientProvider, GitHubEventProcessorAggregator, GitHubPullRequestService,
     *      GitHubPullRequestLineCommentService)
     */
    private final GitHubPullRequestLineCommentService gitHubPullRequestLineCommentService;

    /**
     * Constructor.
     * 
     * @param githubClientProvider
     *            used as the connection
     * @param redirects
     *            functionality to the appropriate event processors
     * @param gitHubPullRequestService
     *            Injected {@link GitHubPullRequestService} dependency.
     * @param gitHubPullRequestCommentService
     *            Injected {@link GitHubPullRequestCommentService} dependency.
     * @param gitHubPullRequestLineCommentService
     *            Injected {@link GitHubPullRequestLineCommentService} dependency.
     * 
     */
    public GitHubRepositoryServiceImpl(//
            GithubClientProvider githubClientProvider, //
            GitHubEventProcessorAggregator<EventPayload> eventProcessorAggregator, //
            GitHubPullRequestService gitHubPullRequestService, //
            GitHubPullRequestCommentService gitHubPullRequestCommentService, //
            GitHubPullRequestLineCommentService gitHubPullRequestLineCommentService //
    )
    {
        this.githubClientProvider = githubClientProvider;
        this.eventProcessorAggregator = eventProcessorAggregator;
        this.gitHubPullRequestService = gitHubPullRequestService;
        this.gitHubPullRequestCommentService = gitHubPullRequestCommentService;
        this.gitHubPullRequestLineCommentService = gitHubPullRequestLineCommentService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void synchronize(Repository repository)
    {
        EventService eventService = githubClientProvider.getEventService(repository);

        // gets repository ID
        RepositoryId repositoryId = RepositoryId.create(repository.getOrgName(), repository.getSlug());

        // goes over events
        Iterator<Collection<Event>> eventsIterator = eventService.pageEvents(repositoryId).iterator();
        while (eventsIterator.hasNext())
        {
            Collection<Event> nextPage = eventsIterator.next();
            for (Event event : nextPage)
            {
                eventProcessorAggregator.process(repository, event);
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
                dump.put(gitHubPullRequestAction.getAt(), //
                        gitHubPullRequestAction.getActor().getName() // name
                                + " " + gitHubPullRequestAction.getAction().name() // action
                                + " the pull request " + gitHubPullRequest.getTitle() // title
                                + " on " + gitHubPullRequestAction.getAt() // date
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
