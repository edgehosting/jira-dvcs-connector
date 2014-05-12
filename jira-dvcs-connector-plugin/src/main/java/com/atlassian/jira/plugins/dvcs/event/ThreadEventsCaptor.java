package com.atlassian.jira.plugins.dvcs.event;

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
    void stopCapturing();

    /**
     * Publishes events to the Atlassian {@code com.atlassian.event.api.EventPublisher}
     */
    void sendToEventPublisher();
}
