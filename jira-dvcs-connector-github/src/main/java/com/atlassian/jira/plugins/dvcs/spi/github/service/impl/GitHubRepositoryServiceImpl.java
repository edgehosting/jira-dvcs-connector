package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.EventPayload;
import org.eclipse.egit.github.core.service.EventService;
import org.eclipse.egit.github.core.service.GitHubService;
import org.eclipse.egit.github.core.service.PullRequestService;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.review.ReviewItem;
import com.atlassian.jira.plugins.dvcs.spi.github.model.review.pullRequest.CreatedPullRequestReviewItem;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessorAggregator;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubRepositoryService;

/**
 * An {@link GitHubService} implementation.
 * 
 * @author stanislav-dvorscak@solumiss.eu
 * 
 */
public class GitHubRepositoryServiceImpl implements GitHubRepositoryService
{

    /**
     * @see #GitHubRepositoryServiceImpl(GithubClientProvider, GitHubEventProcessorAggregator, PullRequestService)
     */
    private final GithubClientProvider githubClientProvider;

    /**
     * @see #GitHubRepositoryServiceImpl(GithubClientProvider, GitHubEventProcessorAggregator, PullRequestService)
     */
    private final GitHubEventProcessorAggregator<EventPayload> eventProcessorAggregator;

    /**
     * @see #GitHubRepositoryServiceImpl(GithubClientProvider, GitHubEventProcessorAggregator, PullRequestService)
     */
    private final GitHubPullRequestService gitHubPullRequestService;

    /**
     * It is used for sorting review items by their timestamp.
     */
    private static final Comparator<ReviewItem> REVIEW_ITEM_COMPARATOR = new Comparator<ReviewItem>()
    {

        @Override
        public int compare(ReviewItem o1, ReviewItem o2)
        {
            return o1.getTimeStamp().compareTo(o2.getTimeStamp());
        }

    };

    /**
     * Constructor.
     * 
     * @param githubClientProvider
     *            used as the connection
     * @param redirects
     *            functionality to the appropriate event processors
     * @param gitHubPullRequestService
     */
    public GitHubRepositoryServiceImpl(GithubClientProvider githubClientProvider,
            GitHubEventProcessorAggregator<EventPayload> eventProcessorAggregator, GitHubPullRequestService gitHubPullRequestService)
    {
        this.githubClientProvider = githubClientProvider;
        this.eventProcessorAggregator = eventProcessorAggregator;
        this.gitHubPullRequestService = gitHubPullRequestService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sync(Repository repository)
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

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ReviewItem> getReview(String issueKey)
    {
        ArrayList<ReviewItem> result = new ArrayList<ReviewItem>();

        for (GitHubPullRequest gitHubPullRequest : gitHubPullRequestService.getGitHubPullRequest(issueKey))
        {
            CreatedPullRequestReviewItem createdPullRequestReviewItem = new CreatedPullRequestReviewItem();
            createdPullRequestReviewItem.setTimeStamp(gitHubPullRequest.getCreatedAt());
            result.add(createdPullRequestReviewItem);
        }

        Collections.sort(result, REVIEW_ITEM_COMPARATOR);

        return result;
    }
}
