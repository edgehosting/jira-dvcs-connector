package com.atlassian.jira.plugins.dvcs.event;

import java.util.Collection;

/**
 * ThreadEventsCapture is used to control capture of events on the current thread.
 *
 * @see ThreadEvents#startCapturingEvents()
 */
public interface ThreadEventsCapture
{
    /**
     * Stops capturing events.
     */
    void stopCapturing();

    /**
     * Publishes events to the provided listeners. The listeners should use the {@link
     * com.google.common.eventbus.EventBus Guava EventBus} <code>{@literal @Subscribe}</code> annotation to indicate
     * what events they listen to.
     *
     * @param listeners a Collection of listeners
     * @see com.google.common.eventbus.EventBus
     */
    void publishTo(Collection<?> listeners);
}
