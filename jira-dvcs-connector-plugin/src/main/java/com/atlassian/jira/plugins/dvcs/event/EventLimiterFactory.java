package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.plugins.dvcs.sync.SyncConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Factor for {@link EventLimiter} instances.
 */
@Component
public class EventLimiterFactory
{
    private final SyncConfig syncConfig;
    private final ApplicationProperties applicationProperties;

    @Autowired
    public EventLimiterFactory(SyncConfig syncConfig, ApplicationProperties applicationProperties)
    {
        this.syncConfig = syncConfig;
        this.applicationProperties = applicationProperties;
    }

    /**
     * Creates a new EventLimiter.
     *
     * @return an EventLimiter
     */
    EventLimiter create()
    {
        return new EventLimiter(syncConfig, applicationProperties);
    }
}
