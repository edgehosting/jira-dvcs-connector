package com.atlassian.jira.plugins.dvcs.sync;

import java.io.IOException;

import javax.annotation.Resource;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.PullRequestReviewCommentPayload;
import org.eclipse.egit.github.core.service.PullRequestService;

import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.message.GitHubPullRequestSynchronizeMessage;
import com.atlassian.jira.plugins.dvcs.spi.github.message.GitHubPullRequestSynchronizeMessage.ChangeType;
import com.atlassian.jira.plugins.dvcs.spi.github.service.AbstractGitHubEventProcessor;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessor;

/**
 * The {@link PullRequestReviewCommentPayload} implementation of the {@link GitHubEventProcessor}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class PullRequestReviewCommentPayloadGitHubEventProcessor extends AbstractGitHubEventProcessor<PullRequestReviewCommentPayload>
{

    /**
     * Injected {@link GithubClientProvider} dependency.
     */
    @Resource
    private GithubClientProvider githubClientProvider;

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
        PullRequestReviewCommentPayload payload = getPayload(event);
        CommitComment commitComment = payload.getComment();
        PullRequest pullRequest = getPullRequestByComment(repository, commitComment);
        if (pullRequest == null)
        {
            return;
        }

        Progress progress = synchronizer.getProgress(repository.getId());
        GitHubPullRequestSynchronizeMessage message = new GitHubPullRequestSynchronizeMessage(progress, progress.getAuditLogId(),
                isSoftSync, repository, pullRequest.getNumber(), ChangeType.PULL_REQUEST_REVIEW_COMMENT);

        messagingService.publish(
                messagingService.get(GitHubPullRequestSynchronizeMessage.class, GitHubPullRequestSynchronizeMessageConsumer.ADDRESS),
                message, synchronizationTags);
    }

    /**
     * Resolves {@link GitHubPullRequest} for the provided comment, currently it is only workaround, because information is not available.
     * 
     * @param domain
     *            repository
     * @param commitComment
     *            for which comment
     * @return resolved {@link GitHubPullRequest}
     */
    private PullRequest getPullRequestByComment(Repository domain, CommitComment commitComment)
    {
        RepositoryId repositoryId = RepositoryId.create(domain.getOrgName(), domain.getSlug());
        try
        {
            PullRequestService pullRequestService = githubClientProvider.getPullRequestService(domain);

            // iterates over open pull requests
            for (PullRequest pullRequest : pullRequestService.getPullRequests(repositoryId, "open"))
            {
                for (CommitComment tmpCommitComment : pullRequestService.getComments(repositoryId, pullRequest.getNumber()))
                {
                    if (commitComment.getId() == tmpCommitComment.getId())
                    {
                        return pullRequest;
                    }
                }
            }

            // iterates over closed pull-requests
            for (PullRequest pullRequest : pullRequestService.getPullRequests(repositoryId, "closed"))
            {
                for (CommitComment tmpCommitComment : pullRequestService.getComments(repositoryId, pullRequest.getNumber()))
                {
                    if (commitComment.getId() == tmpCommitComment.getId())
                    {
                        return pullRequest;
                    }
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
    public Class<PullRequestReviewCommentPayload> getEventPayloadType()
    {
        return PullRequestReviewCommentPayload.class;
    }

}
