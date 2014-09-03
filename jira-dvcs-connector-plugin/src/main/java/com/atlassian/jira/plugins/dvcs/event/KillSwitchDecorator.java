package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Decorator for {@link EventService} that respects the {@link com.atlassian.jira.plugins.dvcs.event.EventsFeature}
 */
@Component
public class KillSwitchDecorator implements CarefulEventService
{
    private final EventsFeature eventsFeature;
    private final EventService delegate;

    @Autowired
    public KillSwitchDecorator(final EventsFeature eventsFeature, final EventService delegate)
    {
        this.eventsFeature = eventsFeature;
        this.delegate = delegate;
    }

    @Override
    public void storeEvent(Repository repository, SyncEvent event) throws IllegalArgumentException
    {
        if (eventsFeature.isEnabled())
        {
            delegate.storeEvent(repository, event);
        }
    }

    @Override
    public void storeEvent(Repository repository, SyncEvent event, boolean scheduled)
            throws IllegalArgumentException
    {
        if (eventsFeature.isEnabled())
        {
            delegate.storeEvent(repository, event, scheduled);
        }
    }

    @Override
    public void storeEvent(final int repositoryId, final SyncEvent event, final boolean scheduled)
            throws IllegalArgumentException
    {
        if (eventsFeature.isEnabled())
        {
            delegate.storeEvent(repositoryId, event, scheduled);
        }
    }

    @Override
    public void dispatchEvents(Repository repository)
    {
        if (eventsFeature.isEnabled())
        {
            delegate.dispatchEvents(repository);
        }
    }

    @Override
    public void dispatchEvents(int repositoryId)
    {
        if (eventsFeature.isEnabled())
        {
            delegate.dispatchEvents(repositoryId);
        }
    }

    @Override
    public void discardEvents(Repository repository)
    {
        if (eventsFeature.isEnabled())
        {
            delegate.discardEvents(repository);
        }
    }
}
