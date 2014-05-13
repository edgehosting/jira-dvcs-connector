package com.atlassian.jira.plugins.dvcs.spi.github.service;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.EventPayload;

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
    public void process(Repository domain, Event event, boolean isSoftSync, String[] synchronizationTags, GitHubEventContext context)
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
