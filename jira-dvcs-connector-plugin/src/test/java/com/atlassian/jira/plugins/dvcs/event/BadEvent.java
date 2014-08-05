package com.atlassian.jira.plugins.dvcs.event;

import java.util.Date;
import javax.annotation.Nonnull;

/**
 * Event that can't be marshalled by Jackson.
 */
class BadEvent implements SyncEvent
{
    private final Date date = new Date();

    @Nonnull
    @Override
    public Date getDate()
    {
        return date;
    }

    public String getUnmarshallableThing()
    {
        throw new RuntimeException("ya can't json me");
    }
}
