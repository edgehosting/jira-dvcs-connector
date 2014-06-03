package com.atlassian.jira.plugins.dvcs.event;

/**
 * Indicates that an event limit has been reached. When an event limit is reached all further events that have the same
 * limit are dropped. This class indicates how many events were dropped in total.
 */
public class LimitExceededEvent
{
    private final int droppedEventCount;

    /**
     * @param droppedEventCount the number of dropped events
     */
    public LimitExceededEvent(int droppedEventCount)
    {
        this.droppedEventCount = droppedEventCount;
    }

    /**
     * @return the number of events that were dropped
     */
    public int getDroppedEventCount()
    {
        return droppedEventCount;
    }
}
