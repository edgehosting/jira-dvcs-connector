package com.atlassian.jira.plugins.dvcs.event;

import javax.annotation.Nonnull;

/**
 * Interface used by events
 */
public interface LimitedEvent
{
    /**
     * Returns the limit that should be applied to this event.
     *
     * @return an EventLimit
     */
    @Nonnull
    EventLimit getEventLimit();
}
