package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;

import java.util.Date;
import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link PullRequest} was created.
 */
@SuppressWarnings ("UnusedDeclaration")
@EventName("jira.dvcsconnector.sync.pullrequest.created")
public final class PullRequestCreatedEvent
{
    @Nonnull
    private final PullRequest pullRequest;

    public PullRequestCreatedEvent(@Nonnull PullRequest pullRequest)
    {
        this.pullRequest = checkNotNull(pullRequest, "pullRequest");
    }

    @Nonnull
    public PullRequest getPullRequest()
    {
        return pullRequest;
    }
}
