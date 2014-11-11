package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.service.AbstractGitHubEventProcessor;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventContext;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.PullRequestPayload;
import org.springframework.stereotype.Component;

/**
 * Processors responsible for processing events, which are about {@link PullRequestPayload}.
 *
 * @author Stanislav Dvorscak
 */
@Component
public class PullRequestPayloadGitHubEventProcessor extends AbstractGitHubEventProcessor<PullRequestPayload>
{
    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Repository repository, Event event, boolean isSoftSync, String[] synchronizationTags, GitHubEventContext context)
    {
        PullRequestPayload payload = getPayload(event);
        PullRequest pullRequest = payload.getPullRequest();

        context.savePullRequest(pullRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<PullRequestPayload> getEventPayloadType()
    {
        return PullRequestPayload.class;
    }

}
