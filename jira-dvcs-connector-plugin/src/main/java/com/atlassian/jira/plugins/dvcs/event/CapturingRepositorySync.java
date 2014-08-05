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
    private final CarefulEventService eventService;
    private final boolean scheduledSync;

    CapturingRepositorySync(@Nonnull CarefulEventService eventService, @Nonnull Repository repository, boolean scheduledSync, @Nonnull ThreadEventsCaptor threadEventCaptor)
    {
        this.eventService = checkNotNull(eventService, "eventService");
        this.repository = checkNotNull(repository, "repository");
        this.scheduledSync = scheduledSync;
        this.threadEventCaptor = checkNotNull(threadEventCaptor, "threadEventsCaptor");
    }

    @Override
    public void finish()
    {
        try
        {
            storeEvents();
        }
        finally
        {
            // do this in a finally block to ensure we stop capturing on this thread
            threadEventCaptor.stopCapturing();
        }
    }

    private void storeEvents()
    {
        threadEventCaptor.processEach(SyncEvent.class, new ThreadEventsCaptor.Closure<SyncEvent>()
        {
            @Override
            public void process(@Nonnull SyncEvent event)
            {
                eventService.storeEvent(repository, event, scheduledSync);
            }
        });
    }
}
