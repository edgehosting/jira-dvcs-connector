package com.atlassian.jira.plugins.dvcs.spi.github.service.impl.event;

import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.PushPayload;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPush;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessor;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPushService;

/**
 * Implementation of the {@link GitHubEventProcessor} for the {@link PushPayload} based event.
 * 
 * @author stanislav-dvorscak@solumiss.eu
 * 
 */
public class PushPayloadGitHubEventProcessor extends AbstractGitHubEventProcessor<PushPayload>
{

    /**
     * @see #PushPayloadGitHubEventProcessor(GitHubPushService)
     */
    private final GitHubPushService gitHubPushService;

    /**
     * Constructor.
     * 
     * @param gitHubPushService
     *            Injected {@link GitHubPushService} dependency.
     */
    public PushPayloadGitHubEventProcessor(final GitHubPushService gitHubPushService)
    {
        this.gitHubPushService = gitHubPushService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Repository repository, Event event)
    {
        PushPayload pushPayload = getPayload(event);

        GitHubPush gitHubPush = new GitHubPush();
        gitHubPush.setBefore(pushPayload.getBefore());
        gitHubPush.setHead(pushPayload.getHead());
        gitHubPush.setRef(pushPayload.getRef());

        gitHubPushService.save(gitHubPush);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<PushPayload> getEventPayloadType()
    {
        return PushPayload.class;
    }

}
