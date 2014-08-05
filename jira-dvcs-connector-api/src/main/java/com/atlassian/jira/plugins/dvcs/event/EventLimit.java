package com.atlassian.jira.plugins.dvcs.event;

import java.util.Locale;
import javax.annotation.Nonnull;

/**
 * Available limits for synchronisation events. The default limits may be overridden by setting an application property
 * with the name <code>dvcs.connector.event.limit.&lt;lowercase_limit_name&gt;</code>.
 */
public enum EventLimit
{
    /**
     * Commit limit (defaults to 100). May be overridden by setting application property
     * <code>dvcs.connector.event.limit.commit</code>.
     */
    COMMIT(100),

    /**
     * Branch limit (defaults to 10). May be overridden by setting application property
     * <code>dvcs.connector.event.limit.branch</code>.
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

    /**
     * @return the name of the application property used to override the default limit
     */
    @Nonnull
    public String getOverrideLimitProperty()
    {
        return "dvcs.connector.event.limit." + name().toLowerCase(Locale.US);
    }
}
