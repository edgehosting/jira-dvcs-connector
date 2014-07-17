package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.GitHubEventMapping;
import com.atlassian.jira.plugins.dvcs.dao.GitHubEventDAO;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.SyncDisabledHelper;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.github.message.GitHubPullRequestPageMessage;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessorAggregator;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventService;
import com.atlassian.jira.plugins.dvcs.sync.GitHubPullRequestPageMessageConsumer;
import com.atlassian.jira.plugins.dvcs.sync.GitHubPullRequestProcessor;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.collect.Iterables;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.client.PagedRequest;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.EventPayload;
import org.eclipse.egit.github.core.service.EventService;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Resource;

/**
 * Implementation of the {@link GitHubEventService}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubEventServiceImpl implements GitHubEventService
{
    /**
     * Injected {@link GitHubEventDAO} dependency.
     */
    @Resource
    private GitHubEventDAO gitHubEventDAO;

    /**
     * Injected {@link GitHubEventProcessorAggregator} dependency.
     */
    @Resource
    private GitHubEventProcessorAggregator<EventPayload> gitHubEventProcessorAggregator;

    /**
     * Injected {@link ActiveObjects} dependency.
     */
    @Resource
    private ActiveObjects activeObjects;

    /**
     * Injected {@link GithubClientProvider} dependency.
     */
    @Resource(name = "githubClientProvider")
    private GithubClientProvider githubClientProvider;

    @Resource
    private Synchronizer synchronizer;

    @Resource
    MessagingService messagingService;

    @Resource
    private SyncDisabledHelper syncDisabledHelper;

    @Resource
    private GitHubPullRequestProcessor gitHubPullRequestProcessor;

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAll(Repository repository)
    {
        gitHubEventDAO.removeAll(repository);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void synchronize(final Repository repository, final boolean isSoftSync, final String[] synchronizationTags)
    {
        EventService eventService = githubClientProvider.getEventService(repository);

        // gets repository ID
        RepositoryId forRepositoryId = RepositoryId.createFromUrl(repository.getRepositoryUrl());

        final GitHubEventMapping lastGitHubEventSavePoint = gitHubEventDAO.getLastSavePoint(repository);

        String latestEventGitHubId = null;
        final GitHubEventContextImpl context = new GitHubEventContextImpl(synchronizer, messagingService, repository, isSoftSync, synchronizationTags);
        PageIterator<Event> events = eventService.pageEvents(forRepositoryId);

        boolean forcePRListSynchronization = false;
        for (final Event event : Iterables.concat(events))
        {
            forcePRListSynchronization = true;
            // processes single event - and returns flag if the processing of next records should be stopped, because their was already
            // proceed
            boolean shouldStop = activeObjects.executeInTransaction(new TransactionCallback<Boolean>()
            {

                @Override
                public Boolean doInTransaction()
                {
                    // before, not before or equals - there can exists several events with the same timestamp, but it does not mean that
                    // all of them was already proceed
                    if (lastGitHubEventSavePoint != null && event.getCreatedAt().before(lastGitHubEventSavePoint.getCreatedAt()))
                    {
                        // all previous records was already proceed - we can stop events' iterating
                        return Boolean.TRUE;

                    }
                    else if (gitHubEventDAO.getByGitHubId(repository, event.getId()) != null)
                    {
                        // maybe partial synchronization, and there can exist remaining events which was fired at the same time
                        // or save point was not marked and there can still exists entries which was not already proceed
                        return Boolean.FALSE;

                    }

                    // called registered GitHub event processors
                    gitHubEventProcessorAggregator.process(repository, event, isSoftSync, synchronizationTags, context);
                    saveEventCounterpart(repository, event, false);

                    return Boolean.FALSE;
                }
            });

            if (shouldStop)
            {
                forcePRListSynchronization = false;
                break;
            }
            else if (latestEventGitHubId == null)
            {
                latestEventGitHubId = event.getId();
            }
        }

        // marks last event as a save point - because all previous records was fully proceed
        if (latestEventGitHubId != null)
        {
            gitHubEventDAO.markAsSavePoint(gitHubEventDAO.getByGitHubId(repository, latestEventGitHubId));
        }

        if (forcePRListSynchronization && !syncDisabledHelper.isGitHubUsePullRequestListDisabled())
        {
            // there could be other updates, lets fetch them form PR list API

            Progress progress = synchronizer.getProgress(repository.getId());

            GitHubPullRequestPageMessage message = new GitHubPullRequestPageMessage(null, progress.getAuditLogId(), progress.isSoftsync(), repository, PagedRequest.PAGE_FIRST, GithubCommunicator.PULLREQUEST_PAGE_SIZE, context.getProcessedPullRequests());
            MessageAddress<GitHubPullRequestPageMessage> key = messagingService.get(
                    GitHubPullRequestPageMessage.class,
                    GitHubPullRequestPageMessageConsumer.ADDRESS
            );
            messagingService.publish(key, message, synchronizationTags);
        }
    }

    /**
     * Stores provided {@link Event} locally as {@link GitHubEventMapping}. It is determined as marker that provided event was already
     * proceed.
     *
     * @param repository
     *            over of event
     * @param event
     *            GitHub event which was proceed
     * @param savePoint
     *            true if it is save point, false otherwise
     */

    private void saveEventCounterpart(Repository repository, Event event, boolean savePoint)
    {
        Map<String, Object> gitHubEvent = new HashMap<String, Object>();
        gitHubEvent.put(GitHubEventMapping.GIT_HUB_ID, event.getId());
        gitHubEvent.put(GitHubEventMapping.CREATED_AT, event.getCreatedAt());
        gitHubEvent.put(GitHubEventMapping.REPOSITORY, repository.getId());
        if (savePoint)
        {
            gitHubEvent.put(GitHubEventMapping.SAVE_POINT, savePoint);
        }
        gitHubEventDAO.create(gitHubEvent);
    }

    @Override
    public void saveEvent(Repository repository, Event event, boolean savePoint)
    {
        // save only if the event is not there
        if (gitHubEventDAO.getByGitHubId(repository, event.getId()) == null)
        {
            saveEventCounterpart(repository, event, savePoint);
        }
    }
}
