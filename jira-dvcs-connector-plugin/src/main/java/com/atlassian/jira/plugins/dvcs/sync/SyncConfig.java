package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.plugins.dvcs.event.EventLimit;
import com.atlassian.util.concurrent.Nullable;
import com.google.common.annotations.VisibleForTesting;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configuration for synchronisation.
 */
@Component
public class SyncConfig
{
    private static final Logger logger = LoggerFactory.getLogger(SyncConfig.class);

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
     * Used to check for limit overrides.
     */
    private final ApplicationProperties applicationProperties;

    @Autowired
    public SyncConfig(ApplicationProperties applicationProperties)
    {
        this.applicationProperties = applicationProperties;
    }

    /**
     * Returns the sync interval that should be used. This defaults to 1 hour and can be overridden by setting the
     * {@value #PROPERTY_KEY} system property to a value in milliseconds.
     */
    public long scheduledSyncIntervalMillis()
    {
        return Long.getLong(PROPERTY_KEY, DEFAULT_INTERVAL.getMillis());
    }

    /**
     * Returns the effective limit for {@code eventLimit}, taking any overrides into account.
     *
     * @param eventLimit the EventLimit to calculate the effective limit for
     * @return the effective limit for {@code eventLimit}
     */
    public int getEffectiveLimit(@Nonnull EventLimit eventLimit)
    {
        checkNotNull(eventLimit, "eventLimit");

        String override = applicationProperties.getString(eventLimit.getOverrideLimitProperty());
        if (override != null)
        {
            try
            {
                return Integer.parseInt(override);
            }
            catch (NumberFormatException e)
            {
                logger.warn("Ignoring invalid limit override for {}: {}", eventLimit, override);
            }
        }

        return eventLimit.getDefaultLimit();
    }

    /**
     * Overrides the default limit for {@code eventLimit} with {@code newLimit}.
     *
     * @param eventLimit an EventLimit
     * @param newLimit an optional new limit to use (passing {@code null} resets to the default value)
     */
    public void setEffectiveLimit(@Nonnull EventLimit eventLimit, @Nullable Integer newLimit)
    {
        checkNotNull(eventLimit, "eventLimit");
        applicationProperties.setString(eventLimit.getOverrideLimitProperty(), newLimit != null ? newLimit.toString() : null);
    }
}
