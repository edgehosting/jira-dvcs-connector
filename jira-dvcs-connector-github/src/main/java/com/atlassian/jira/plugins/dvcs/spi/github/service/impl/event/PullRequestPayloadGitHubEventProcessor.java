package com.atlassian.jira.plugins.dvcs.spi.github.service.impl.event;

import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.PullRequestPayload;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestAction;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestAction.Action;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessor;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubUserService;

/**
 * An {@link PullRequestPayload} based {@link GitHubEventProcessor}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class PullRequestPayloadGitHubEventProcessor extends AbstractGitHubEventProcessor<PullRequestPayload>
{

    /**
     * @see #PullRequestPayloadGitHubEventProcessor(GithubClientProvider, GitHubPullRequestService, GitHubUserService)
     */
    private final GitHubPullRequestService gitHubPullRequestService;

    /**
     * @see #PullRequestPayloadGitHubEventProcessor(GithubClientProvider, GitHubPullRequestService, GitHubUserService)
     */
    private final GitHubUserService gitHubUserService;

    /**
     * Constructor.
     * 
     * @param gitHubPullRequestService
     *            Injected {@link GitHubPullRequestService} dependency.
     * @param gitHubUserService
     *            Injected {@link GitHubUserService} dependency.
     */
    public PullRequestPayloadGitHubEventProcessor(GitHubPullRequestService gitHubPullRequestService, GitHubUserService gitHubUserService)
    {
        this.gitHubPullRequestService = gitHubPullRequestService;
        this.gitHubUserService = gitHubUserService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Repository repository, Event event)
    {
        PullRequestPayload payload = getPayload(event);
        PullRequest pullRequest = payload.getPullRequest();

        GitHubPullRequest gitHubPullRequest = gitHubPullRequestService
                .synchronize(repository, pullRequest.getId(), pullRequest.getNumber());

        Action resolvedAction = resolveAction(payload);
        // was resolved appropriate action? with other words is supported action?
        if (resolvedAction != null)
        {

            // reuse action if already synchronized
            GitHubPullRequestAction action = null;
            for (GitHubPullRequestAction oldAction : gitHubPullRequest.getActions())
            {
                if (event.getId().equals(oldAction.getGitHubEventId()))
                {
                    action = oldAction;
                    break;
                }
            }

            // creates news one
            if (action == null)
            {
                action = new GitHubPullRequestAction();
                action.setGitHubEventId(event.getId());
                gitHubPullRequest.getActions().add(action);
            }

            action.setAt(event.getCreatedAt());
            action.setActor(gitHubUserService.synchronize(event.getActor().getLogin(), repository));
            action.setAction(resolvedAction);
        }

        gitHubPullRequestService.save(gitHubPullRequest);
    }

    /**
     * Resolves action ENUM for the provided {@link PullRequestPayload}.
     * 
     * @param payload
     * @return resolved action
     */
    private Action resolveAction(PullRequestPayload payload)
    {
        Action result = null;

        if ("opened".equalsIgnoreCase(payload.getAction()))
        {
            result = GitHubPullRequestAction.Action.OPENED;

        } else if ("closed".equalsIgnoreCase(payload.getAction()))
        {
            if (payload.getPullRequest().getMergedAt() != null)
            {
                result = GitHubPullRequestAction.Action.MERGED;

            } else
            {
                result = GitHubPullRequestAction.Action.CLOSED;

            }

        } else if ("reopened".equalsIgnoreCase(payload.getAction()))
        {
            result = GitHubPullRequestAction.Action.REOPENED;

        }

        return result;
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
