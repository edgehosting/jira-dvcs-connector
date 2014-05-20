package com.atlassian.jira.plugins.dvcs.event;

import javax.annotation.Nonnull;

/**
 * An EventCaptor is used to capture of events on the current thread.
 *
 * @see ThreadEvents#startCapturing()
 */
public interface ThreadEventsCaptor
{
    /**
     * Stops capturing events.
     */
    @Nonnull
    ThreadEventsCaptor stopCapturing();

    /**
     * Calls the given pseudo-closure once for each captured event, removing the event from this captor once it has been
     * processed.
     */
    void processEach(Closure closure);

    /**
     * Pseudo-closure for processing events.
     */
    public interface Closure
    {
        void process(@Nonnull Object event);
    }
}
