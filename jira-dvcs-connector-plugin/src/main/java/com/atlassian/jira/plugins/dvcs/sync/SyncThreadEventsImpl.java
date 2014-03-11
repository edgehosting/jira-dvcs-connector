package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.event.ThreadEvents;
import com.atlassian.jira.plugins.dvcs.event.ThreadEventsCapture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SyncThreadEventsImpl implements SyncThreadEvents
{
    /**
     * Used to listen for events.
     */
    private final ThreadEvents threadEvents;

    /**
     * Provider for sync event listeners.
     */
    private final SyncEventListenerProvider listenersProvider;

    /**
     * Creates a new SyncThreadEventsImpl, registering it with the {@code EventPublisher}.
     *
     * @param threadEvents the EntityEvents
     */
    @Autowired
    public SyncThreadEventsImpl(ThreadEvents threadEvents, SyncEventListenerProvider listenersProvider)
    {
        this.threadEvents = threadEvents;
        this.listenersProvider = listenersProvider;
    }

    @Override
    public SyncEvents startCapturing()
    {
        return new SyncEventsImpl();
    }

    public class SyncEventsImpl implements SyncEvents
    {
        private final ThreadEventsCapture threadEventsCapture = threadEvents.startCapturingEvents();

        private SyncEventsImpl()
        {
            // prevent instantiation outside this class
        }

        @Override
        public void publish()
        {
            threadEventsCapture.publishTo(listenersProvider.getAll());
        }

        @Override
        public void stopCapturing()
        {
            threadEventsCapture.stopCapturing();
        }
    }
}
