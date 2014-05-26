package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Date;
import java.util.Set;
import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link com.atlassian.jira.plugins.dvcs.model.Changeset} was created.
 */
@SuppressWarnings ("UnusedDeclaration")
@EventName ("jira.dvcsconnector.sync.changeset.created")
public final class ChangesetCreatedEvent implements SyncEvent
{
    @Nonnull
    private final Changeset changeset;

    @Nonnull
    private final Set<String> issueKeys;

    public ChangesetCreatedEvent(final @Nonnull Changeset changeset, final @Nonnull Set<String> issueKeys)
    {
        this.issueKeys = checkNotNull(issueKeys, "issueKeys");
        this.changeset = checkNotNull(changeset, "changeset");
    }

    @Nonnull
    public Changeset getChangeset()
    {
        return changeset;
    }

    @Nonnull
    public Set<String> getIssueKeys()
    {
        return issueKeys;
    }

    /**
     * @return the Date when the changeset was created
     */
    @Nonnull
    @Override
    @JsonIgnore
    public Date getDate()
    {
        return changeset.getDate();
    }

    @JsonCreator
    private static ChangesetCreatedEvent fromJSON(@JsonProperty ("changeset") Changeset changeset, @JsonProperty ("issueKeys") Set<String> issueKeys)
    {
        return new ChangesetCreatedEvent(changeset, issueKeys);
    }
}
