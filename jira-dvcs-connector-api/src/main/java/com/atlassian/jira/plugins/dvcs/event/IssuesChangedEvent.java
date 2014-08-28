package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.analytics.api.annotations.EventName;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Date;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * A change was detected for the Issue Keys
 */
@EventName ("jira.dvcsconnector.sync.issue.changed")
public final class IssuesChangedEvent implements SyncEvent
{
    @Nonnull
    private final Date date;

    @Nonnull
    private final Set<String> issueKeys;

    @Nonnull
    private final int repositoryId;

    public IssuesChangedEvent(@Nonnull final int repositoryId, final @Nonnull Set<String> issueKeys)
    {
        this.issueKeys = issueKeys;
        this.date = new Date();
        this.repositoryId = repositoryId;
    }

    public IssuesChangedEvent(@Nonnull final int repositoryId, @Nonnull final Set<String> issueKeys, @Nonnull final Date date)
    {
        this.date = date;
        this.issueKeys = issueKeys;
        this.repositoryId = repositoryId;
    }

    @Nonnull
    public int getRepositoryId()
    {
        return repositoryId;
    }

    @Nonnull
    public Set<String> getIssueKeys()
    {
        return issueKeys;
    }

    @Nonnull
    @Override
    public Date getDate()
    {
        return date;
    }

    @JsonCreator
    private static IssuesChangedEvent fromJSON(@JsonProperty ("repositoryId") int repositoryId,
            @JsonProperty ("issueKeys") Set<String> issueKeys, @JsonProperty ("date") Date date)
    {
        return new IssuesChangedEvent(repositoryId, issueKeys, date);
    }
}
