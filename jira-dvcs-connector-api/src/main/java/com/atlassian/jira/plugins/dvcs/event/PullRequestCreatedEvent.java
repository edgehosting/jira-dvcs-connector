package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Date;
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

    @Nonnull
    @Override
    @JsonIgnore
    public Date getDate()
    {
        return getPullRequest().getCreatedOn();
    }

    @JsonCreator
    private static PullRequestCreatedEvent fromJSON(@JsonProperty ("pullRequest") PullRequest pullRequest)
    {
        return new PullRequestCreatedEvent(pullRequest);
    }
}
