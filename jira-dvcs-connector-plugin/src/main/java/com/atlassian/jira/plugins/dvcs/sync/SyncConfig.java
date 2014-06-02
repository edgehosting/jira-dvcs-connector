package com.atlassian.jira.plugins.dvcs.sync;

import com.google.common.annotations.VisibleForTesting;
import org.joda.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Configuration for synchronisation.
 */
@Component
public class SyncConfig
{
    /**
     * The name of the system property used to override the default sync interval (in milliseconds).
     */
    @VisibleForTesting
    static final String PROPERTY_KEY = "dvcs.connector.scheduler.interval";

    /**
     * The default sync interval.
     */
    private static final Duration DEFAULT_INTERVAL = Duration.standardHours(1);

    /**
     * Returns the sync interval that should be used. This defaults to 1 hour and can be overridden by setting the
     * {@value #PROPERTY_KEY} system property to a value in milliseconds.
     */
    public long scheduledSyncIntervalMillis()
    {
        return Long.getLong(PROPERTY_KEY, DEFAULT_INTERVAL.getMillis());
    }
}
