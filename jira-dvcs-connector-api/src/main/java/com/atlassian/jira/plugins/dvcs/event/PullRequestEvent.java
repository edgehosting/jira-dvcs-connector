package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import org.apache.commons.lang.builder.ToStringBuilder;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang.builder.ToStringStyle.SHORT_PREFIX_STYLE;

/**
 * Base class for {@code PullRequest} events.
 *
 * @since 2.1.6
 */
public abstract class PullRequestEvent implements SyncEvent
{
    @Nonnull
    protected final PullRequest pullRequest;

    public PullRequestEvent(@Nonnull final PullRequest pullRequest)
    {
        this.pullRequest = checkNotNull(pullRequest, "pullRequest");
    }

    @Nonnull
    public PullRequest getPullRequest()
    {
        return pullRequest;
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }
}
