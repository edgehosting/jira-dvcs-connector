package com.atlassian.jira.plugins.dvcs.event;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

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
    private final ThreadLocal<ThreadEventsCaptorImpl> threadEventCaptor = new ThreadLocal<ThreadEventsCaptorImpl>();

    public ThreadEvents()
    {
    }

    /**
     * Returns an EventsCapture instance that can be used to capture and publish events on the current thread. Captured
     * events can be processed using {@link ThreadEventsCaptor#processEach(ThreadEventsCaptor.Closure)}.
     * <p>
     * Remember to <b>call {@code ThreadEventsCaptor.stopCapturing()} to terminate the capture</b> or risk leaking memory.
     *
     * @return a new EventsCapture
     * @throws java.lang.IllegalStateException if there is already an active ThreadEventsCapture on the current thread
     */
    @Nonnull
    public ThreadEventsCaptor startCapturing()
    {
        if (threadEventCaptor.get() != null)
        {
            // we could chain these up but YAGNI... just error out for now
            throw new IllegalStateException("There is already an active ThreadEventsCapture");
        }

        return new ThreadEventsCaptorImpl();
    }

    /**
     * Broadcasts the given event.
     *
     * @param event an event
     */
    public void broadcast(Object event)
    {
        ThreadEventsCaptorImpl eventCaptor = threadEventCaptor.get();
        if (eventCaptor != null)
        {
            eventCaptor.capture(event);
        }
        else
        {
            logger.debug("There is no active ThreadEventsCaptor. Dropping event: {}", event);
        }
    }

    private final class ThreadEventsCaptorImpl implements ThreadEventsCaptor
    {
        /**
         * Where we hold captured events until they are published or discarded.
         */
        @Nonnull
        private List<Object> capturedEvents = Lists.newArrayList();

        /**
         * Creates a new ThreadEventsCapture and sets it as the active capture in the enclosing ThreadEvents.
         */
        ThreadEventsCaptorImpl()
        {
            threadEventCaptor.set(this);
        }

        @Nonnull
        @Override
        public ThreadEventsCaptor stopCapturing()
        {
            threadEventCaptor.remove();
            return this;
        }

        @Override
        public void processEach(@Nonnull Closure<Object> closure)
        {
            processEach(Object.class, closure);
        }

        @Override
        public <T> void processEach(@Nonnull Class<T> eventClass, @Nonnull Closure<? super T> closure)
        {
            checkNotNull(eventClass, "eventClass");
            checkNotNull(closure, "closure");

            final List<?> all = ImmutableList.copyOf(capturedEvents);
            for (Object object : all)
            {
                if (eventClass.isInstance(object))
                {
                    T event = eventClass.cast(object);

                    logger.debug("Processing event with {}: {}", closure, event);
                    closure.process(event);

                    // remove processed events from the list so that in case the
                    // closure above throws an exception we are still in a valid state.
                    capturedEvents.remove(event);
                }
            }

            logger.debug("Processed {} events of type {} with {}", new Object[] { all.size() - capturedEvents.size(), eventClass, closure });
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
