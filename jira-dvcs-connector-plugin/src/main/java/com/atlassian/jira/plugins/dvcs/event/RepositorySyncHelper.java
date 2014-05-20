package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper class for managing events during repository synchronisation.
 */
@Component
public class RepositorySyncHelper
{
    private static RepositorySync NULL_REPO_SYNC = new NullRepositorySync();

    private final ThreadEvents threadEvents;
    private final EventService eventService;

    @Autowired
    public RepositorySyncHelper(@Nonnull ThreadEvents threadEvents, @Nonnull EventService eventService)
    {
        this.threadEvents = checkNotNull(threadEvents, "threadEvents");
        this.eventService = checkNotNull(eventService, "eventService");
    }

    /**
     * Returns a new RepositorySync object for the given repository. If {@code repository} is null or {@code softSync}
     * is false the returned RepositorySync will not capture (or store) events.
     *
     * @param repository the Repository being synchronised
     * @param softSync whether the synchronisation is a soft sync
     * @return a RepositorySync
     */
    @Nonnull
    public RepositorySync startSync(@Nullable Repository repository, boolean softSync)
    {
        if (repository != null && softSync)
        {
            return new CapturingRepositorySync(eventService, repository, threadEvents.startCapturing());
        }

        return NULL_REPO_SYNC;
    }
}
