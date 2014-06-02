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
     * Calls the given pseudo-closure once for each captured event, removing the event from this captor's list of
     * captured event once it has been processed.
     */
    void processEach(@Nonnull Closure<Object> closure);

    /**
     * Calls the given pseudo-closure once for each captured event that is an instance of {@code eventClass}, removing the
     * event from this captor's list of captured event once it has been processed.
     */
    <T> void processEach(@Nonnull Class<T> eventClass, @Nonnull Closure<? super T> closure);

    /**
     * Pseudo-closure for processing events.
     */
    public interface Closure<T>
    {
        void process(@Nonnull T event);
    }
}
