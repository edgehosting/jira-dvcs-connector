package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.jira.plugins.dvcs.model.Branch;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Date;
import java.util.Set;
import javax.annotation.Nonnull;

import static com.atlassian.jira.plugins.dvcs.event.EventLimit.BRANCH;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link com.atlassian.jira.plugins.dvcs.model.Branch} was created.
 */
@SuppressWarnings ("UnusedDeclaration")
@EventName ("jira.dvcsconnector.sync.branch.created")
public final class BranchCreatedEvent implements SyncEvent, LimitedEvent
{
    @Nonnull
    private final Branch branch;

    @Nonnull
    private final Set<String> issueKeys;

    @Nonnull
    private final Date date;

    public BranchCreatedEvent(final @Nonnull Branch branch, final @Nonnull Set<String> issueKeys)
    {
        this(branch, issueKeys, new Date());
    }

    public BranchCreatedEvent(final @Nonnull Branch branch, final @Nonnull Set<String> issueKeys, final @Nonnull Date date)
    {
        this.branch = checkNotNull(branch, "branch");
        this.issueKeys = checkNotNull(issueKeys, "issueKeys");
        this.date = checkNotNull(date, "date");
    }

    @Nonnull
    public Branch getBranch()
    {
        return branch;
    }

    @Nonnull
    public Set<String> getIssueKeys()
    {
        return issueKeys;
    }

    /**
     * Returns the branch creation date. Unfortunately we don't have a way to determine when a branch was created so we
     * just return the date on which this event instance was created.
     *
     * @return the date on which this event was created
     */
    @Nonnull
    @Override
    public Date getDate()
    {
        return date;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public EventLimit getEventLimit()
    {
        return BRANCH;
    }

    @Override
    public String toString()
    {
        return "BranchCreatedEvent{branch=" + branch + ", issueKeys=" + issueKeys + ", date=" + date + '}';
    }

    @JsonCreator
    private static BranchCreatedEvent fromJSON(@JsonProperty ("branch") Branch branch,
            @JsonProperty ("issueKeys") Set<String> issueKeys, @JsonProperty ("date") Date date)
    {
        return new BranchCreatedEvent(branch, issueKeys, date);
    }
}
