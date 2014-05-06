package com.atlassian.jira.plugins.dvcs.event;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * Thread-local event bus.
 */
@Component
public class ThreadEvents
{
    private static final Logger logger = LoggerFactory.getLogger(ThreadEvents.class);

    /**
     * Captures thread-local events until they are published or discarded.
     */
    private final ThreadLocal<EventsCapture> threadEventsCapture = new ThreadLocal<EventsCapture>();

    public ThreadEvents()
    {
    }

    /**
     * Returns an EventsCapture instance that can be used to capture and publish events on the current thread. Captured
     * events can be published using {@link com.atlassian.jira.plugins.dvcs.event.ThreadEvents.EventsCapture#publishTo(java.util.Collection)}.
     * <p/>
     * Remember to <b>call {@code EventsCapture.stopCapturing()} to terminate the capture</b> or risk leaking memory.
     *
     * @return a new EventsCapture
     * @throws java.lang.IllegalStateException if there is already an active ThreadEventsCapture on the current thread
     */
    public ThreadEventsCapture startCapturingEvents()
    {
        if (threadEventsCapture.get() != null)
        {
            // we could chain these up but YAGNI... just error out for now
            throw new IllegalStateException("There is already an active ThreadEventsCapture");
        }

        return new EventsCapture();
    }

    /**
     * Broadcasts the given event.
     *
     * @param event an event
     */
    public void broadcast(Object event)
    {
        EventsCapture eventsCapture = threadEventsCapture.get();

        if (eventsCapture != null)
        {
            eventsCapture.capture(event);
        }
        else {
            logger.debug("There is no active ThreadEventsCapture. Dropping event: {}", event);
        }
    }

    private final class EventsCapture implements ThreadEventsCapture
    {
        /**
         * Where we hold captured events until they are published or discarded.
         */
        @Nonnull
        private List<Object> capturedEvents = Lists.newArrayList();

        /**
         * Creates a new ThreadEventsCapture and sets it as the active capture in the enclosing ThreadEvents.
         */
        EventsCapture()
        {
            threadEventsCapture.set(this);
        }

        @Override
        public void stopCapturing()
        {
            threadEventsCapture.remove();
        }

        @Override
        public void publishTo(Collection<?> listeners)
        {
            // create a new event bus just for publishing
            final EventBus eventBus = new EventBus();
            for (Object listener : listeners)
            {
                logger.debug("Registering listener: {}", listener);
                eventBus.register(listener);
            }

            for (Object event : capturedEvents)
            {
                logger.debug("Posting event: {}", event);
                eventBus.post(event);
            }

            logger.debug("Published {} events to {}", capturedEvents.size(), listeners);
            capturedEvents = Lists.newArrayList();
        }

        /**
         * Captures an event that was raised while this EventsCapture is active.
         *
         * @param event an event
         */
        void capture(final Object event)
        {
            logger.debug("Capturing event: {}", event);
            capturedEvents.add(event);
        }
    }
}
