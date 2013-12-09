package com.atlassian.jira.plugins.dvcs.sync;

import javax.annotation.Resource;

import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.PullRequestPayload;

import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.spi.github.message.GitHubPullRequestSynchronizeMessage;
import com.atlassian.jira.plugins.dvcs.spi.github.message.GitHubPullRequestSynchronizeMessage.ChangeType;
import com.atlassian.jira.plugins.dvcs.spi.github.service.AbstractGitHubEventProcessor;

/**
 * Processors responsible for processing events, which are about {@link PullRequestPayload}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class PullRequestPayloadGitHubEventProcessor extends AbstractGitHubEventProcessor<PullRequestPayload>
{

    /**
     * Injected {@link MessagingService} dependency.
     */
    @Resource
    private MessagingService messagingService;

    /**
     * Injected {@link Synchronizer} dependency.
     */
    @Resource
    private Synchronizer synchronizer;

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Repository repository, Event event, boolean isSoftSync, String[] synchronizationTags)
    {
        PullRequestPayload payload = getPayload(event);
        PullRequest pullRequest = payload.getPullRequest();

        Progress progress = synchronizer.getProgress(repository.getId());
        GitHubPullRequestSynchronizeMessage message = new GitHubPullRequestSynchronizeMessage(progress, progress.getAuditLogId(),
                isSoftSync, repository, pullRequest.getNumber(), ChangeType.PULL_REQUEST);
        messagingService.publish(
                messagingService.get(GitHubPullRequestSynchronizeMessage.class, GitHubPullRequestSynchronizeMessageConsumer.ADDRESS),
                message, synchronizationTags);
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
