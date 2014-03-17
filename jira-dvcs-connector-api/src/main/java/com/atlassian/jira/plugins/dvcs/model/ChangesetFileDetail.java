package com.atlassian.jira.plugins.dvcs.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Changeset detailed file information.
 */
public class ChangesetFileDetail extends ChangesetFile
{
    private final int additions;
    private final int deletions;

    @JsonCreator
    public ChangesetFileDetail(@JsonProperty("fileAction") ChangesetFileAction fileAction, @JsonProperty("file") String file, @JsonProperty("additions") int additions, @JsonProperty("deletions") int deletions)
    {
        super(fileAction, file);
        this.additions = additions;
        this.deletions = deletions;
    }

    @JsonProperty
    public int getAdditions()
    {
        return additions;
    }

    @JsonProperty
    public int getDeletions()
    {
        return deletions;
    }

    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(final Object o)
    {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
