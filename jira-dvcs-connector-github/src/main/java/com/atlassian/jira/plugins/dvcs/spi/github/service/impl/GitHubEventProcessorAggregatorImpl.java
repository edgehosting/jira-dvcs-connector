package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventContext;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessor;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessorAggregator;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.EventPayload;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aggregator over all {@link GitHubEventProcessor}s.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubEventProcessorAggregatorImpl implements GitHubEventProcessorAggregator<EventPayload>
{

    /**
     * An {@link EventPayload} type to the appropriate processors.
     */
    private final Map<Class<? extends EventPayload>, GitHubEventProcessor<? extends EventPayload>> eventProcessors = new ConcurrentHashMap<Class<? extends EventPayload>, GitHubEventProcessor<? extends EventPayload>>();

    /**
     * Cache of the already resolved processors. The synchronization is done via direct object locking.
     */
    private final Map<Class<? extends EventPayload>, GitHubEventProcessor<? extends EventPayload>> resolvedProcessorCache = new ConcurrentHashMap<Class<? extends EventPayload>, GitHubEventProcessor<? extends EventPayload>>();

    /**
     * Constructor.
     * 
     * @param eventProcessors
     *            available event processor
     */
    public GitHubEventProcessorAggregatorImpl(GitHubEventProcessor<? extends EventPayload>... eventProcessors)
    {
        for (GitHubEventProcessor<? extends EventPayload> eventProcessor : eventProcessors)
        {
            this.eventProcessors.put(eventProcessor.getEventPayloadType(), eventProcessor);
        }
    }

    /**
     * Resolves the most concrete {@link Event} processor for the provider type of the {@link EventPayload} type.
     * 
     * @param eventPayloadType
     * @return
     */
    @SuppressWarnings("unchecked")
    private <T_EventPayload extends EventPayload> GitHubEventProcessor<T_EventPayload> resolveEventProcessor(
            Class<? extends T_EventPayload> eventPayloadType)
    {
        GitHubEventProcessor<T_EventPayload> result;

        result = (GitHubEventProcessor<T_EventPayload>) resolvedProcessorCache.get(eventPayloadType);
        if (result != null)
        {
            return result;
        }

        synchronized (resolvedProcessorCache)
        {
            result = (GitHubEventProcessor<T_EventPayload>) resolvedProcessorCache.get(eventPayloadType);
            if (result != null)
            {
                return result;
            }

            for (Entry<Class<? extends EventPayload>, GitHubEventProcessor<? extends EventPayload>> entry : eventProcessors.entrySet())
            {
                if (eventPayloadType.isAssignableFrom(entry.getKey()))
                {
                    result = (GitHubEventProcessor<T_EventPayload>) entry.getValue();
                    resolvedProcessorCache.put(eventPayloadType, result);
                    return result;
                }
            }

            // if no handler was found for this type, check super EventType
            if (EventPayload.class.isAssignableFrom(eventPayloadType.getSuperclass()))
            {
                resolveEventProcessor((Class<? extends T_EventPayload>) eventPayloadType.getSuperclass());
            }

            // by default there is no event processor - it means null
            return null;
        }
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
