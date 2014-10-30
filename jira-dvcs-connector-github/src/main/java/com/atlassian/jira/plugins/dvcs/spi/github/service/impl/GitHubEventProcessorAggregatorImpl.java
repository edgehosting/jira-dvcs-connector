package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventContext;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessor;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessorAggregator;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.EventPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aggregator over all {@link GitHubEventProcessor}s.
 *
 * @author Stanislav Dvorscak
 */
@Component
public class GitHubEventProcessorAggregatorImpl implements GitHubEventProcessorAggregator<EventPayload>
{
    /**
     * Cache of the already resolved processors. The synchronization is done via direct object locking.
     */
    private final Map<Class<? extends EventPayload>, GitHubEventProcessor<? extends EventPayload>> eventProcessorsMapping = new ConcurrentHashMap<Class<? extends EventPayload>, GitHubEventProcessor<? extends EventPayload>>();

    @Autowired
    public GitHubEventProcessorAggregatorImpl(GitHubEventProcessor<? extends EventPayload>... eventProcessors)
    {
        for (GitHubEventProcessor<? extends EventPayload> eventProcessor : eventProcessors)
        {
            eventProcessorsMapping.put(eventProcessor.getEventPayloadType(), eventProcessor);
        }
    }

    /**
     * Resolves the most concrete {@link Event} processor for the provider type of the {@link EventPayload} type.
     */
    @SuppressWarnings ("unchecked")
    private <T_EventPayload extends EventPayload> GitHubEventProcessor<T_EventPayload> resolveEventProcessor(
            Class<? extends T_EventPayload> eventPayloadType)
    {
        // try resolve the event processor from cache
        GitHubEventProcessor<T_EventPayload> result = (GitHubEventProcessor<T_EventPayload>) eventProcessorsMapping.get(eventPayloadType);
        if (result != null)
        {
            return result;
        }

        // if no handler was found for this type, check super EventType
        if (EventPayload.class.isAssignableFrom(eventPayloadType.getSuperclass()))
        {
            result = resolveEventProcessor((Class<? extends T_EventPayload>) eventPayloadType.getSuperclass());
            eventProcessorsMapping.put(eventPayloadType, result);
            return result;
        }

        // by default there is no event processor - it means null
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Repository domain, Event event, boolean isSoftSync, String[] synchronizationTags, GitHubEventContext context)
    {
        GitHubEventProcessor<EventPayload> resolvedEventProcessor = resolveEventProcessor(event.getPayload().getClass());
        if (resolvedEventProcessor != null)
        {
            resolvedEventProcessor.process(domain, event, isSoftSync, synchronizationTags, context);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<EventPayload> getEventPayloadType()
    {
        return EventPayload.class;
    }

}
