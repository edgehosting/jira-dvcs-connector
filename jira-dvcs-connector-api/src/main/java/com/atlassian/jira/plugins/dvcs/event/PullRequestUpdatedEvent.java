package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Date;
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
    @Override
    @JsonIgnore
    public Date getDate()
    {
        return getPullRequest().getUpdatedOn();
    }

    @Nonnull
    public PullRequest getPullRequestBeforeUpdate()
    {
        return pullRequestBeforeUpdate;
    }

    @JsonCreator
    private static PullRequestUpdatedEvent fromJSON(@JsonProperty("pullRequest") PullRequest pullRequest, @JsonProperty("pullRequestBeforeUpdate") PullRequest pullRequestBeforeUpdate)
    {
        return new PullRequestUpdatedEvent(pullRequest, pullRequestBeforeUpdate);
    }
}
