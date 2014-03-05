package com.atlassian.jira.plugins.dvcs.event;

import org.springframework.stereotype.Component;

/**
 * Thread-local event bus.
 */
@Component
public class ThreadEvents
{
    /**
     * Captures thread-local events until they are published or discarded.
     */
    private final ThreadLocal<ThreadEventsCapture> threadEventCapture = new ThreadLocal<ThreadEventsCapture>();

    public ThreadEvents()
    {
    }

    /**
     * Returns an EventsCapture instance that can be used to capture and publish events on the current thread. Captured
     * events can be published using {@link ThreadEventsCapture#publishTo(java.util.Collection)}.
     * <p/>
     * Remember to <b>call {@code EventsCapture.stopCapturing()} to terminate the capture</b> or risk leaking memory.
     *
     * @return a new EventsCapture
     */
    public ThreadEventsCapture startCapturingEvents()
    {
        return new ThreadEventsCapture(this);
    }

    /**
     * Broadcasts the given event.
     *
     * @param event an event
     */
    public void broadcast(Object event)
    {
        ThreadEventsCapture threadEventsCapture = threadEventCapture.get();
        if (threadEventsCapture != null)
        {
            threadEventsCapture.capture(event);
        }
    }

    /**
     * Sets the EventsCapture for the current thread.
     *
     * @param threadEventsCapture an EventsCapture
     */
    void setThreadEventCapture(ThreadEventsCapture threadEventsCapture)
    {
        if (threadEventCapture.get() != null)
        {
            // we could chain these up but YAGNI... just error out for now
            throw new IllegalStateException("There is already an active EventsCapture");
        }

        threadEventCapture.set(threadEventsCapture);
    }

    /**
     * Unsets the EventsCapture for the current thread.
     */
    void unsetThreadEventsCapture()
    {
        threadEventCapture.remove();
    }
}
