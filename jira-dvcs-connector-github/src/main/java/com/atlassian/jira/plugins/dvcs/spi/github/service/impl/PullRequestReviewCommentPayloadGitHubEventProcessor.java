package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.service.AbstractGitHubEventProcessor;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventContext;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessor;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.PullRequestReviewCommentPayload;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import javax.annotation.Resource;

/**
 * The {@link PullRequestReviewCommentPayload} implementation of the {@link GitHubEventProcessor}.
 *
 * @author Stanislav Dvorscak
 */
@Component
public class PullRequestReviewCommentPayloadGitHubEventProcessor
        extends AbstractGitHubEventProcessor<PullRequestReviewCommentPayload>
{

    /**
     * Injected {@link GithubClientProvider} dependency.
     */
    @Resource
    private GithubClientProvider githubClientProvider;

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Repository repository, Event event, boolean isSoftSync, String[] synchronizationTags, GitHubEventContext context)
    {
        PullRequestReviewCommentPayload payload = getPayload(event);
        CommitComment commitComment = payload.getComment();
        PullRequest pullRequest = getPullRequestByComment(repository, commitComment);
        if (pullRequest == null)
        {
            return;
        }

        context.savePullRequest(pullRequest);
    }

    /**
     * Resolves {@link PullRequest} for the provided comment, currently it is only workaround, because information is
     * not available.
     *
     * @param domain repository
     * @param commitComment for which comment
     * @return resolved {@link PullRequest}
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

        }
        catch (IOException e)
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
