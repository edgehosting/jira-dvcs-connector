package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.service.AbstractGitHubEventProcessor;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventContext;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.PullRequestPayload;

/**
 * Processors responsible for processing events, which are about {@link PullRequestPayload}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
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

        context.savePullRequest(pullRequest.getId(), pullRequest.getNumber());
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
