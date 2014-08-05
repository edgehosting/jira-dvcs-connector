package com.atlassian.jira.plugins.dvcs.event;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Date;
import javax.annotation.Nonnull;

import static org.apache.commons.lang.builder.ToStringStyle.SHORT_PREFIX_STYLE;

/**
 * Sync event for testing.
 */
class TestEvent implements SyncEvent
{
    private final Date date;
    private final String data;

    public TestEvent()
    {
        this(new Date(), "random-data");
    }

    public TestEvent(Date date, String data)
    {
        this.date = date;
        this.data = data;
    }

    @Nonnull
    @Override
    public Date getDate()
    {
        return date;
    }

    public String getData()
    {
        return data;
    }

    @Override
    public boolean equals(final Object obj)
    {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }
}
