package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.service.AbstractGitHubEventProcessor;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventContext;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.IssueCommentPayload;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import javax.annotation.Resource;

/**
 * The {@link IssueCommentPayload} event processor.
 *
 * @author Stanislav Dvorscak
 */
@Component
public class IssueCommentPayloadEventProcessor extends AbstractGitHubEventProcessor<IssueCommentPayload>
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
        IssueCommentPayload payload = getPayload(event);

        // if payload doesn't contain information about issue we can stop
        if (payload.getIssue() == null)
        {
            return;
        }

        PullRequest pullRequest = payload.getIssue().getPullRequest();

        // it can happen that the issue is not related to pull request (only issue is created)
        // and the repository is null here
        if (pullRequest == null)
        {
            return;
        }

        // reloads PR-s by HTML URL because PR of issue's comment does not contains any other PR informations
        pullRequest = getPullRequestByHtmlUrl(repository, pullRequest.getHtmlUrl());
        if (pullRequest == null)
        {
            return;
        }

        context.savePullRequest(pullRequest);
    }

    /**
     * Resolves {@link PullRequest} for the provided pull request PR HTML URL.
     *
     * @param repository PR owner
     * @param htmlUrl of the {@link PullRequest}
     * @return resolved {@link PullRequest}
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
    public Class<IssueCommentPayload> getEventPayloadType()
    {
        return IssueCommentPayload.class;
    }

}
