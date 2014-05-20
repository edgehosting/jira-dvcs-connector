package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Simple HashMap-backed event service. This will not be shipped to customers but is useful so we can keep running the
 * CI regression tests and also for testing locally.
 */
@Component
public class SimpletonEventService implements EventService
{
    private final EventPublisher eventPublisher;
    private final Multimap<String, Object> syncEventsByRepo = ArrayListMultimap.create();

    @Autowired
    public SimpletonEventService(EventPublisher eventPublisher)
    {
        this.eventPublisher = eventPublisher;
    }

    public synchronized void storeEvent(Repository repository, Object event) throws IllegalArgumentException
    {
        syncEventsByRepo.put(repository.getRepositoryUrl(), event);
    }

    public synchronized void dispatchEvents(Repository repository)
    {
        for (Object event : syncEventsByRepo.get(repository.getRepositoryUrl()))
        {
            eventPublisher.publish(event);
        }
    }
}
