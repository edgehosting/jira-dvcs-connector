package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.EventPayload;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessor;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessorAggregator;

/**
 * Aggregator over all {@link GitHubEventProcessor}s.
 * 
 * @author stanislav-dvorscak@solumiss.eu
 * 
 */
public class GitHubEventProcessorAggregatorImpl implements GitHubEventProcessorAggregator<EventPayload>
{

	/**
	 * An {@link EventPayload} type to the appropriate processors.
	 */
	private final Map<Class<? extends EventPayload>, GitHubEventProcessor<? extends EventPayload>> eventProcessors = new ConcurrentHashMap<Class<? extends EventPayload>, GitHubEventProcessor<? extends EventPayload>>();

	/**
	 * Cache of the already resolved processors. It is weak hash map - because class can be proxy class, which otherwise can result into the
	 * memory leak. The synchronization is done via direct object locking.
	 */
	private final Map<Class<? extends EventPayload>, GitHubEventProcessor<? extends EventPayload>> resolvedProcessorCache = new WeakHashMap<Class<? extends EventPayload>, GitHubEventProcessor<? extends EventPayload>>();

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

		synchronized (resolvedProcessorCache)
		{
			result = (GitHubEventProcessor<T_EventPayload>) resolvedProcessorCache.get(eventPayloadType);
		}

		if (result != null)
		{
			return result;
		}

		for (Entry<Class<? extends EventPayload>, GitHubEventProcessor<? extends EventPayload>> entry : eventProcessors.entrySet())
		{
			if (entry.getKey().isAssignableFrom(eventPayloadType))
			{
				result = (GitHubEventProcessor<T_EventPayload>) entry.getValue();

				synchronized (resolvedProcessorCache)
				{
					resolvedProcessorCache.put(eventPayloadType, result);
				}

				return result;
			}
		}

		Class<? extends T_EventPayload> superEventPayloadType = eventPayloadType.getSuperclass().isAssignableFrom(eventPayloadType) ? (Class<? extends T_EventPayload>) eventPayloadType
				.getSuperclass() : null;

		// by default there is no event processor - it means null
		return superEventPayloadType != null ? resolveEventProcessor(superEventPayloadType) : null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void process(Repository repository, Event event)
	{
		GitHubEventProcessor<EventPayload> resolvedEventProcessor = resolveEventProcessor(event.getPayload().getClass());
		if (resolvedEventProcessor != null)
		{
			resolvedEventProcessor.process(repository, event);
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