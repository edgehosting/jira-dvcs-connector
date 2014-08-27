package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Set;
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

    @Nonnull
    protected final Set<String> issueKeys;

    public PullRequestEvent(@Nonnull final PullRequest pullRequest, @Nonnull final Set<String> issueKeys)
    {
        this.pullRequest = checkNotNull(pullRequest, "pullRequest");
        this.issueKeys = checkNotNull(issueKeys, "issueKeys");
    }

    @Nonnull
    public PullRequest getPullRequest()
    {
        return pullRequest;
    }

    @Nonnull
    public Set<String> getIssueKeys()
    {
        return issueKeys;
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }
}
