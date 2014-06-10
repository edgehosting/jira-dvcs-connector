package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.plugins.dvcs.event.EventLimit;
import com.atlassian.util.concurrent.LazyReference;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

    /**
     * Changing the system property after the job has been scheduled does not have any effect so we read it
     * once and use that value throughout the lifetime of the plugin.
     */
    private final LazyReference<Long> scheduledSyncInterval = new LazyReference<Long>()
    {
        @Override
        protected Long create() throws Exception
        {
            return Long.getLong(PROPERTY_KEY, DEFAULT_INTERVAL.getMillis());
        }
    };

    @Autowired
    public SyncConfig(ApplicationProperties applicationProperties)
    {
        this.applicationProperties = applicationProperties;
    }

    /**
     * Returns the sync interval that should be used. This defaults to 1 hour and can be overridden by setting the
     * {@value #PROPERTY_KEY} system property to a value in milliseconds.
     * <p/>
     * Note that changing the system property after the plugin has started has no effect.
     */
    public long scheduledSyncIntervalMillis()
    {
        //noinspection ConstantConditions
        return scheduledSyncInterval.get();
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

        Optional<Integer> appProperty = optInteger(eventLimit, applicationProperties.getString(eventLimit.getOverrideLimitProperty()));
        if (appProperty.isPresent())
        {
            return appProperty.get();
        }

        Optional<Integer> sysProperty = optInteger(eventLimit, System.getProperty(eventLimit.getOverrideLimitProperty()));
        if (sysProperty.isPresent())
        {
            return sysProperty.get();
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

    private static Optional<Integer> optInteger(EventLimit eventLimit, @Nullable String override)
    {
        if (override != null)
        {
            try
            {
                return Optional.of(Integer.parseInt(override));
            }
            catch (NumberFormatException e)
            {
                logger.warn("Ignoring invalid limit override for {}: {}", eventLimit, override);
            }
        }

        return Optional.absent();
    }
}
