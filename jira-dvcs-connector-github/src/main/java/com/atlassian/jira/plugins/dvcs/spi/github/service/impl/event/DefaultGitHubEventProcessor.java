package com.atlassian.jira.plugins.dvcs.spi.github.service.impl.event;

import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.EventPayload;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessor;

/**
 * Default implementation of the {@link GitHubEventProcessor}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class DefaultGitHubEventProcessor implements GitHubEventProcessor<EventPayload>
{

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Repository repository, Event event)
    {
        // nothing to do
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
