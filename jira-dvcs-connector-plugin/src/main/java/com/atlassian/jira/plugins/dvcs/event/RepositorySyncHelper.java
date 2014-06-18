package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag.SOFT_SYNC;
import static com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag.WEBHOOK_SYNC;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper class for managing events during repository synchronisation.
 */
@Component
public class RepositorySyncHelper
{
    private static RepositorySync NULL_REPO_SYNC = new NullRepositorySync();

    private final ThreadEvents threadEvents;
    private final CarefulEventService eventService;

    @Autowired
    public RepositorySyncHelper(@Nonnull ThreadEvents threadEvents, @Nonnull CarefulEventService eventService)
    {
        this.threadEvents = checkNotNull(threadEvents, "threadEvents");
        this.eventService = checkNotNull(eventService, "eventService");
    }

    /**
     * Returns a new RepositorySync object for the given repository. If {@code repository} is null or {@code softSync}
     * is false the returned RepositorySync will not capture (or store) events.
     *
     * @param repository the Repository being synchronised
     * @param syncFlags synchronisation flags
     * @return a RepositorySync
     */
    @Nonnull
    public RepositorySync startSync(@Nullable Repository repository, @Nonnull EnumSet<SynchronizationFlag> syncFlags)
    {
        checkNotNull(syncFlags, "syncFlags");
        if (repository != null && syncFlags.contains(SOFT_SYNC))
        {
            return new CapturingRepositorySync(eventService, repository, !syncFlags.contains(WEBHOOK_SYNC), threadEvents.startCapturing());
        }

        return NULL_REPO_SYNC;
    }
}
