package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;

import java.util.EnumSet;
import javax.annotation.Nonnull;

import static com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag.WEBHOOK_SYNC;

/**
 * Limited event for testing.
 */
public class LimitedTestEvent extends TestEvent implements LimitedEvent
{
    private final EventLimit eventLimit;
    private final boolean scheduledSync;

    public LimitedTestEvent(EventLimit eventLimit, boolean scheduledSync)
    {
        this.eventLimit = eventLimit;
        this.scheduledSync = scheduledSync;
    }

    @Nonnull
    @Override
    public EnumSet<SynchronizationFlag> getSyncFlags()
    {
        return scheduledSync ? EnumSet.noneOf(SynchronizationFlag.class) : EnumSet.of(WEBHOOK_SYNC);
    }

    @Nonnull
    @Override
    public EventLimit getEventLimit()
    {
        return eventLimit;
    }
}
