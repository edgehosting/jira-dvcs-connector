package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.spi.github.message.GitHubPullRequestSynchronizeMessage;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventContext;
import com.atlassian.jira.plugins.dvcs.sync.GitHubPullRequestSynchronizeMessageConsumer;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import org.eclipse.egit.github.core.PullRequest;

import java.util.HashSet;
import java.util.Set;

/**
 * Context for GitHub event synchronisation
 *
 */
public class GitHubEventContextImpl implements GitHubEventContext
{
    private final Synchronizer synchronizer;
    private final MessagingService messagingService;

    private final Repository repository;
    private final boolean isSoftSync;
    private final String[] synchronizationTags;
    private final boolean webHookSync;

    private final Set<Long> processedPullRequests = new HashSet<Long>();

    public GitHubEventContextImpl(final Synchronizer synchronizer, final MessagingService messagingService, final Repository repository, final boolean softSync, final String[] synchronizationTags, boolean webHookSync)
    {
        this.synchronizer = synchronizer;
        this.messagingService = messagingService;
        this.repository = repository;
        isSoftSync = softSync;
        this.synchronizationTags = synchronizationTags;
        this.webHookSync = webHookSync;
    }

    @Override
    public void savePullRequest(PullRequest pullRequest)
    {
        if (pullRequest == null || processedPullRequests.contains(pullRequest.getId()))
        {
            return;
        }

        processedPullRequests.add(pullRequest.getId());

        Progress progress = synchronizer.getProgress(repository.getId());
        GitHubPullRequestSynchronizeMessage message = new GitHubPullRequestSynchronizeMessage(progress, progress.getAuditLogId(),
                isSoftSync, repository, pullRequest.getNumber(), webHookSync);

        messagingService.publish(
                messagingService.get(GitHubPullRequestSynchronizeMessage.class, GitHubPullRequestSynchronizeMessageConsumer.ADDRESS),
                message, synchronizationTags);
    }

    public Set<Long> getProcessedPullRequests()
    {
        return processedPullRequests;
    }
}
