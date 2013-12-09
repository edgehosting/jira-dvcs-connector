package com.atlassian.jira.plugins.dvcs.sync;

import java.io.IOException;

import javax.annotation.Resource;

import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.IssueCommentPayload;
import org.eclipse.egit.github.core.service.PullRequestService;

import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.message.GitHubPullRequestSynchronizeMessage;
import com.atlassian.jira.plugins.dvcs.spi.github.message.GitHubPullRequestSynchronizeMessage.ChangeType;
import com.atlassian.jira.plugins.dvcs.spi.github.service.AbstractGitHubEventProcessor;

/**
 * The {@link IssueCommentPayload} event processor.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class IssueCommentPayloadEventProcessor extends AbstractGitHubEventProcessor<IssueCommentPayload>
{

    /**
     * Injected {@link MessagingService} dependency.
     */
    @Resource
    MessagingService messagingService;

    /**
     * Injected {@link Synchronizer} dependency.
     */
    @Resource
    private Synchronizer synchronizer;

    /**
     * Injected {@link GithubClientProvider} dependency.
     */
    @Resource
    private GithubClientProvider githubClientProvider;

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Repository repository, Event event, boolean isSoftSync, String[] synchronizationTags)
    {
        IssueCommentPayload payload = getPayload(event);

        PullRequest pullRequest = payload.getIssue().getPullRequest();

        // reloads PR-s by HTML URL because PR of issue's comment does not contains any other PR informations
        pullRequest = getPullRequestByHtmlUrl(repository, pullRequest.getHtmlUrl());
        if (pullRequest == null)
        {
            return;
        }

        Progress progress = synchronizer.getProgress(repository.getId());
        GitHubPullRequestSynchronizeMessage message = new GitHubPullRequestSynchronizeMessage(progress, progress.getAuditLogId(),
                isSoftSync, repository, pullRequest.getNumber(), ChangeType.PULL_REQUEST_COMMENT);

        messagingService.publish(
                messagingService.get(GitHubPullRequestSynchronizeMessage.class, GitHubPullRequestSynchronizeMessageConsumer.ADDRESS),
                message, synchronizationTags);
    }

    /**
     * Resolves {@link PullRequest} for the provided pull request PR HTML URL.
     * 
     * @param domain
     *            over which repository
     * @param repository
     *            PR owner
     * @param htmlUrl
     *            of the {@link PullRequest}
     * @return resolved {@link GitHubPullRequest}
     */
    private PullRequest getPullRequestByHtmlUrl(Repository repository, String htmlUrl)
    {
        PullRequestService pullRequestService = githubClientProvider.getPullRequestService(repository);
        RepositoryId repositoryId = RepositoryId.createFromUrl(repository.getRepositoryUrl());

        try
        {
            // iterates over open pull requests
            for (PullRequest pullRequest : pullRequestService.getPullRequests(repositoryId, "open"))
            {
                if (pullRequest.getHtmlUrl().equals(htmlUrl))
                {
                    return pullRequest;
                }
            }

            // iterates over closed pull-requests
            for (PullRequest pullRequest : pullRequestService.getPullRequests(repositoryId, "closed"))
            {
                if (pullRequest.getHtmlUrl().equals(htmlUrl))
                {
                    return pullRequest;
                }
            }

            return null;

        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<IssueCommentPayload> getEventPayloadType()
    {
        return IssueCommentPayload.class;
    }

}
