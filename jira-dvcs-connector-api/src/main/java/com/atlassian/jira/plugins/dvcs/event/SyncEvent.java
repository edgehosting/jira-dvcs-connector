package com.atlassian.jira.plugins.dvcs.event;

import java.util.Date;
import javax.annotation.Nonnull;

/**
 * A synchronisation event (must be usable by Jackson to convert to/from JSON) with a date.
 */
public interface SyncEvent
{
    /**
     * @return the Date when the event occurred
     */
    @Nonnull
    public Date getDate();
}
