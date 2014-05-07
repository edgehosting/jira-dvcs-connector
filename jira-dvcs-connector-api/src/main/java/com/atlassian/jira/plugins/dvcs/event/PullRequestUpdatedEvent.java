package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;

import javax.annotation.Nonnull;

/**
 * A {@link PullRequest} was updated.
 *
 * @since 2.1.6
 */
@SuppressWarnings ("UnusedDeclaration")
@EventName ("jira.dvcsconnector.sync.pullrequest.updated")
public class PullRequestUpdatedEvent extends PullRequestEvent
{
    @Nonnull
    private final PullRequest pullRequestBeforeUpdate;

    public PullRequestUpdatedEvent(@Nonnull final PullRequest pullRequest, @Nonnull final PullRequest pullRequestBeforeUpdate)
    {
        super(pullRequest);
        this.pullRequestBeforeUpdate = pullRequestBeforeUpdate;
    }

    @Nonnull
    public PullRequest getPullRequestBeforeUpdate()
    {
        return pullRequestBeforeUpdate;
    }
}
