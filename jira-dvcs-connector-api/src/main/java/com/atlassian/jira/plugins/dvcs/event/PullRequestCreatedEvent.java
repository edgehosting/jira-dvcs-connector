package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;

import javax.annotation.Nonnull;

/**
 * A {@link PullRequest} was created.
 */
@SuppressWarnings ("UnusedDeclaration")
@EventName("jira.dvcsconnector.sync.pullrequest.created")
public final class PullRequestCreatedEvent extends PullRequestEvent
{
    public PullRequestCreatedEvent(@Nonnull PullRequest pullRequest)
    {
        super(pullRequest);
    }
}
