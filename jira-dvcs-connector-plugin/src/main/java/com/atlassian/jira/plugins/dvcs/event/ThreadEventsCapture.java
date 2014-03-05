package com.atlassian.jira.plugins.dvcs.event;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * EventsCapture is used to control capture of events on the current thread.
 *
 * @see ThreadEvents#startCapturingEvents()
 */
public class ThreadEventsCapture
{
    /**
     * Used to set and unset this instance as the active capture.
     */
    private final ThreadEvents threadEvents;

    /**
     * Where we hold captured events until they are published or discarded.
     */
    @Nonnull
    private List<Object> capturedEvents = Lists.newArrayList();

    /**
     * Creates a new EventsCapture and sets it as the active capture in ActiveObjectsEvents.
     *
     * @param threadEvents an ActiveObjectsEvents instance
     */
    ThreadEventsCapture(ThreadEvents threadEvents)
    {
        this.threadEvents = threadEvents;
        this.threadEvents.setThreadEventCapture(this);
    }

    /**
     * Stop capturing events.
     */
    public void stopCapturing()
    {
        threadEvents.unsetThreadEventsCapture();
    }

    /**
     * Publish events to the provided listeners. The listeners should use the {@link
     * com.google.common.eventbus.EventBus Guava EventBus} <code>{@literal @Subscribe}</code> annotation to indicate
     * what events they listen to.
     *
     * @param listeners a Collection of listeners
     * @see com.google.common.eventbus.EventBus
     */
    public void publishTo(Collection<?> listeners)
    {
        // create a new event bus just for publishing
        final EventBus eventBus = new EventBus();
        for (Object listener : listeners)
        {
            eventBus.register(listener);
        }

        for (Object event : capturedEvents)
        {
            eventBus.post(event);
        }

        capturedEvents = Lists.newArrayList();
    }

    /**
     * Captures an event that was raised while this EventsCapture is active.
     *
     * @param event an event
     */
    void capture(final Object event)
    {
        capturedEvents.add(event);
    }
}
