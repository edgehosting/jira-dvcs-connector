package com.atlassian.jira.plugins.dvcs.event;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Date;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * A change was detected for the Issue Keys
 */
public final class DevSummaryChangedEvent implements SyncEvent
{
    @Nonnull
    private final Date date;

    @Nonnull
    private final Set<String> issueKeys;

    @Nonnull
    private final int repositoryId;

    @Nonnull
    private final String dvcsType;

    public DevSummaryChangedEvent(@Nonnull final int repositoryId, final String dvcsType, final @Nonnull Set<String> issueKeys)
    {
        this(repositoryId, dvcsType, issueKeys, new Date());
    }

    public DevSummaryChangedEvent(@Nonnull final int repositoryId, @Nonnull final String dvcsType, @Nonnull final Set<String> issueKeys, @Nonnull final Date date)
    {
        this.date = date;
        this.issueKeys = issueKeys;
        this.repositoryId = repositoryId;
        this.dvcsType = dvcsType;
    }

    @Nonnull
    public String getDvcsType()
    {
        return dvcsType;
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
    private static DevSummaryChangedEvent fromJSON(@JsonProperty ("repositoryId") int repositoryId,
            @JsonProperty ("dvcsType") String dvcsType, @JsonProperty ("issueKeys") Set<String> issueKeys,
            @JsonProperty ("date") Date date)
    {
        return new DevSummaryChangedEvent(repositoryId, dvcsType, issueKeys, date);
    }
}
