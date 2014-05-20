package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.plugins.dvcs.model.Repository;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper class for capturing/storing events produced during repository synchronisation.
 */
class CapturingRepositorySync implements RepositorySync
{
    private final Repository repository;
    private final ThreadEventsCaptor threadEventCaptor;
    private final EventService eventService;

    CapturingRepositorySync(@Nonnull EventService eventService, @Nonnull Repository repository, @Nonnull ThreadEventsCaptor threadEventCaptor)
    {
        this.eventService = checkNotNull(eventService, "eventService");
        this.repository = checkNotNull(repository, "repository");
        this.threadEventCaptor = checkNotNull(threadEventCaptor, "threadEventsCaptor");
    }

    @Override
    @Nonnull
    public RepositorySync storeEvents()
    {
        threadEventCaptor.processEach(new ThreadEventsCaptor.Closure()
        {
            @Override
            public void process(@Nonnull Object event)
            {
                eventService.storeEvent(repository, event);
            }
        });

        return this;
    }

    @Override
    public void finishSync()
    {
        threadEventCaptor.stopCapturing();
    }
}
