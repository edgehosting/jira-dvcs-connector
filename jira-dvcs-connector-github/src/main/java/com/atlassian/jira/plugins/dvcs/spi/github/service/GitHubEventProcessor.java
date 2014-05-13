package com.atlassian.jira.plugins.dvcs.spi.github.service;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.EventPayload;

/**
 * Defines the contract for the GitHub event processing.
 * 
 * @author Stanislav Dvorscak
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
     * @param isSoftSync
     *            is soft synchronization?
     * @param synchronizationTags
     *            tags of current synchronization
     * @param context
     *            context for GitHub event synchronization
     */
    void process(Repository repository, Event event, boolean isSoftSync, String[] synchronizationTags, GitHubEventContext context);

    /**
     * @return The type of the payload which is supported by this processor.
     */
    Class<T_Payload> getEventPayloadType();

}
