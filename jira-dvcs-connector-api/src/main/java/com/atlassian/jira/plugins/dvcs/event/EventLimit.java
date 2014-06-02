package com.atlassian.jira.plugins.dvcs.event;

/**
 * Available limits for synchronisation events.
 */
public enum EventLimit
{
    /**
     * Commit limit.
     */
    COMMIT(100),

    /**
     * Branch limit.
     */
    BRANCH(10);

    /**
     * Default limit for sync events of this type (events/minute).
     */
    private final int defaultLimit;

    /**
     * @param defaultLimit the default limit (events/minute)
     */
    private EventLimit(int defaultLimit)
    {
        this.defaultLimit = defaultLimit;
    }

    /**
     * Returns the default maximum number of events per minute.
     *
     * @return the default limit (events/minute)
     */
    public int getDefaultLimit()
    {
        return defaultLimit;
    }
}
