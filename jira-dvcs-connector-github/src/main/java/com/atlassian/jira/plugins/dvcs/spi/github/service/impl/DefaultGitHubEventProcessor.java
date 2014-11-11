package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventContext;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessor;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.EventPayload;
import org.springframework.stereotype.Component;

/**
 * Default implementation of the {@link com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessor}.
 *
 * @author Stanislav Dvorscak
 */
@Component
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
