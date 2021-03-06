package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.spi.github.CustomPullRequestService;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.message.GitHubPullRequestPageMessage;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventService;
import com.google.common.collect.Iterables;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.service.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.Resource;

import static com.atlassian.jira.plugins.dvcs.spi.github.CustomPullRequestService.DIRECTION_ASC;
import static com.atlassian.jira.plugins.dvcs.spi.github.CustomPullRequestService.DIRECTION_DESC;
import static com.atlassian.jira.plugins.dvcs.spi.github.CustomPullRequestService.SORT_CREATED;
import static com.atlassian.jira.plugins.dvcs.spi.github.CustomPullRequestService.SORT_UPDATED;
import static com.atlassian.jira.plugins.dvcs.spi.github.CustomPullRequestService.STATE_ALL;

/**
 * Message consumer for {@link com.atlassian.jira.plugins.dvcs.spi.github.message.GitHubPullRequestPageMessage}
 */
@Component
public class GitHubPullRequestPageMessageConsumer implements MessageConsumer<GitHubPullRequestPageMessage>
{
    private final Logger log = LoggerFactory.getLogger(GitHubPullRequestPageMessageConsumer.class);

    public static final String QUEUE = GitHubPullRequestPageMessageConsumer.class.getCanonicalName();
    public static final String ADDRESS = GitHubPullRequestPageMessageConsumer.class.getCanonicalName();

    /**
     * Injected {@link com.atlassian.jira.plugins.dvcs.service.message.MessagingService} dependency.
     */
    @Resource
    private MessagingService messagingService;

    /**
     * Injected {@link com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider} dependency.
     */
    @Resource (name = "githubClientProvider")
    private GithubClientProvider gitHubClientProvider;

    /**
     * Injected {@link com.atlassian.jira.plugins.dvcs.sync.GitHubPullRequestProcessor} dependency.
     */
    @Resource
    private GitHubPullRequestProcessor gitHubPullRequestProcessor;

    /**
     * Injected {@link GitHubEventService} dependency.
     */
    @Resource
    private GitHubEventService gitHubEventService;

    @Override
    public void onReceive(final Message<GitHubPullRequestPageMessage> message, final GitHubPullRequestPageMessage payload)
    {
        Repository repository = payload.getRepository();
        int page = payload.getPage();
        int pagelen = payload.getPagelen();
        boolean softSync = payload.isSoftSync();
        Set<Long> processedPullRequests = payload.getProcessedPullRequests();

        final Object[] debugArguments = { page, pagelen, softSync, repository == null ? "" : repository.getName(), processedPullRequests == null ? -1 : processedPullRequests.size() };
        log.debug("processing GitHubPullRequestPageMessage at page {} with a page length of {} and softSync of {} in repository {} with this many already processed {}", debugArguments);

        RepositoryId repositoryId = RepositoryId.createFromUrl(repository.getRepositoryUrl());

        if (page == 1 && !softSync)
        {
            EventService eventService = gitHubClientProvider.getEventService(repository);

            // saving the first event as save point
            // GitHub doesn't support per_page parameter for events, therefore 30 events will be downloaded and saved
            // leaving the page size set to 1 in case that this will change in the future to request only the first event
            PageIterator<Event> eventsPages = eventService.pageEvents(repositoryId, 1);
            boolean savePoint = true;
            for (Event event : Iterables.getFirst(eventsPages, Collections.<Event>emptyList()))
            {
                gitHubEventService.saveEvent(repository, event, savePoint);
                savePoint = false;
            }
        }

        CustomPullRequestService pullRequestService = gitHubClientProvider.getPullRequestService(repository);

        // sorting by update date to be able to stop for soft sync and by creation date when full sync to avoid page shifting
        PageIterator<PullRequest> pullRequestsPages = softSync ?
                pullRequestService.pagePullRequests(repositoryId, STATE_ALL, SORT_UPDATED, DIRECTION_DESC, page, pagelen) :
                pullRequestService.pagePullRequests(repositoryId, STATE_ALL, SORT_CREATED, DIRECTION_ASC, page, pagelen);

        Iterable<PullRequest> pullRequests = Iterables.getFirst(pullRequestsPages, Collections.<PullRequest>emptyList());
        Set<Long> currentlyProccessedPullRequests = new LinkedHashSet<Long>();

        for (PullRequest pullRequest : pullRequests)
        {
            if (processedPullRequests != null && processedPullRequests.contains(pullRequest.getId()))
            {
                continue;
            }
            currentlyProccessedPullRequests.add(pullRequest.getId());
            if (!gitHubPullRequestProcessor.processPullRequestIfNeeded(repository, pullRequest))
            {
                return;
            }
        }

        if (pullRequestsPages.hasNext())
        {
            if (processedPullRequests != null)
            {
                currentlyProccessedPullRequests.addAll(processedPullRequests);
            }
            fireNextPage(message, payload, pullRequestsPages.getNextPage(), currentlyProccessedPullRequests);
        }
    }

    private void fireNextPage(Message<GitHubPullRequestPageMessage> message, GitHubPullRequestPageMessage payload, int nextPage, Set<Long> proccessedPullRequests)
    {
        GitHubPullRequestPageMessage nextMessage = new GitHubPullRequestPageMessage(
                payload.getProgress(),
                payload.getSyncAuditId(),
                payload.isSoftSync(),
                payload.getRepository(),
                nextPage,
                payload.getPagelen(),
                proccessedPullRequests,
                payload.isWebHookSync());
        messagingService.publish(getAddress(), nextMessage, message.getTags());
    }

    @Override
    public String getQueue()
    {
        return QUEUE;
    }

    @Override
    public MessageAddress<GitHubPullRequestPageMessage> getAddress()
    {
        return messagingService.get(GitHubPullRequestPageMessage.class, ADDRESS);
    }

    @Override
    public int getParallelThreads()
    {
        return MessageConsumer.THREADS_PER_CONSUMER;
    }
}
