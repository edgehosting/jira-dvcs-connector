package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;

import java.util.EnumSet;
import javax.annotation.Nonnull;

/**
 * Interface used by events
 */
public interface LimitedEvent
{
    /**
     * Returns the sync flags that apply to this event.
     *
     * @return a set of SynchronizationFlag
     */
    @Nonnull
    EnumSet<SynchronizationFlag> getSyncFlags();

    /**
     * Returns the limit that should be applied to this event.
     *
     * @return an EventLimit
     */
    @Nonnull
    EventLimit getEventLimit();
}
