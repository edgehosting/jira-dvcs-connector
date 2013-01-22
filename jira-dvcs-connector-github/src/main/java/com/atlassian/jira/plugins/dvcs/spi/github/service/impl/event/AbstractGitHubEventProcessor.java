package com.atlassian.jira.plugins.dvcs.spi.github.service.impl.event;

import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.EventPayload;

import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessor;

/**
 * General functionality useful for the {@link GitHubEventProcessor} implementation.
 * 
 * @author stanislav-dvorscak@solumiss.eu
 * 
 * @param <T_EventPayload>
 *            {@link #getEventPayloadType()}
 */
public abstract class AbstractGitHubEventProcessor<T_EventPayload extends EventPayload> implements GitHubEventProcessor<T_EventPayload>
{

	/**
	 * @return Returns true if the event was already proceed and can be skipped.
	 */
	protected boolean wasAlreadyProceed()
	{
		// FIXME<stanislav-dvorscak@solumiss.eu>
		return false;
	}

	/**
	 * @param event
	 *            current proceed event
	 * @return Casted version of the {@link Event#getPayload()}.
	 */
	@SuppressWarnings("unchecked")
	protected T_EventPayload getPayload(Event event)
	{
		return (T_EventPayload) event.getPayload();
	}

}
