package com.atlassian.jira.plugins.dvcs.spi.github.service;

import org.eclipse.egit.github.core.event.EventPayload;


/**
 * It is tagging interface, which mark aggregators of the several {@link GitHubEventProcessor}s.
 * 
 * @author Stanislav Dvorscak
 * 
 * @param <T_EventPayload>
 *            {@link #getEventPayloadType()}
 */
public interface GitHubEventProcessorAggregator<T_EventPayload extends EventPayload> extends GitHubEventProcessor<T_EventPayload>
{
	// NOTHING! IT IS TAGGING INTERFACE
}
