package com.atlassian.jira.plugins.dvcs.event;

import org.codehaus.jackson.annotate.JsonIgnore;

import javax.annotation.Nonnull;

/**
 * Limited event for testing.
 */
public class LimitedTestEvent extends TestEvent implements LimitedEvent
{
    private final EventLimit eventLimit;

    public LimitedTestEvent(EventLimit eventLimit)
    {
        this.eventLimit = eventLimit;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public EventLimit getEventLimit()
    {
        return eventLimit;
    }
}
