package com.atlassian.jira.plugins.dvcs.spi.github.service;

import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.EventPayload;

import com.atlassian.jira.plugins.dvcs.model.Repository;

/**
 * Defines the contract for the GitHub event processing.
 * 
 * @author stanislav-dvorscak@solumiss.eu
 * 
 * @param <T_Payload>
 *            type of the payload
 */
public interface GitHubEventProcessor<T_Payload extends EventPayload>
{

	/**
	 * Processes incoming event.
	 * 
	 * @param repository
	 *            current proceed repository
	 * @param event
	 *            to process
	 */
	void process(Repository repository, Event event);

	/**
	 * @return The type of the payload which is supported by this processor.
	 */
	Class<T_Payload> getEventPayloadType();

}
