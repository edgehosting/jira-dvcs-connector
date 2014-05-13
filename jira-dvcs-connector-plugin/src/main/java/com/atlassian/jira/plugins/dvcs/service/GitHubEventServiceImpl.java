package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.GitHubEventMapping;
import com.atlassian.jira.plugins.dvcs.dao.GitHubEventDAO;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessorAggregator;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventService;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.sal.api.transaction.TransactionCallback;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.EventPayload;
import org.eclipse.egit.github.core.service.EventService;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
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

        String lastProceedEventGitHubId = null;
        final GitHubEventContextImpl context = new GitHubEventContextImpl(synchronizer, messagingService, repository, isSoftSync, synchronizationTags);
        Iterator<Collection<Event>> eventsIterator = eventService.pageEvents(forRepositoryId).iterator();
        while (eventsIterator.hasNext())
        {
            final Collection<Event> nextPage = eventsIterator.next();

            for (final Event event : nextPage)
            {
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

                        } else if (gitHubEventDAO.getByGitHubId(repository, event.getId()) != null)
                        {
                            // maybe partial synchronization, and there can exist remaining events which was fired at the same time
                            // or save point was not marked and there can still exists entries which was not already proceed
                            return Boolean.FALSE;

                        }

                        // called registered GitHub event processors
                        gitHubEventProcessorAggregator.process(repository, event, isSoftSync, synchronizationTags, context);
                        saveEventCounterpart(repository, event);

                        return Boolean.FALSE;
                    }
                });
                lastProceedEventGitHubId = event.getId();

                if (shouldStop)
                {
                    break;
                }
            }

        }

        // marks last event as a save point - because all previous records was fully proceed
        if (lastProceedEventGitHubId != null)
        {
            gitHubEventDAO.markAsSavePoint(gitHubEventDAO.getByGitHubId(repository, lastProceedEventGitHubId));
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
     */
    private void saveEventCounterpart(Repository repository, Event event)
    {
        Map<String, Object> gitHubEvent = new HashMap<String, Object>();
        gitHubEvent.put(GitHubEventMapping.GIT_HUB_ID, event.getId());
        gitHubEvent.put(GitHubEventMapping.CREATED_AT, event.getCreatedAt());
        gitHubEvent.put(GitHubEventMapping.REPOSITORY, repository.getId());
        gitHubEventDAO.create(gitHubEvent);
    }

}
