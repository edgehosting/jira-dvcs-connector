package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.config.FeatureManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Used to determine whether to publish sync events.
 */
@Component
public class EventsFeature
{
    @VisibleForTesting
    static final String FEATURE_NAME = "dvcs.connector.events.disabled";

    private final FeatureManager featureManager;

    @Autowired
    public EventsFeature(@ComponentImport final FeatureManager featureManager)
    {
        this.featureManager = checkNotNull(featureManager);
    }

    /**
     * Returns true unless the "<code>{@value #FEATURE_NAME}</code>" kill switch is flipped.
     *
     * @return whether to publish synchronisation events
     */
    public boolean isEnabled()
    {
        return !featureManager.isEnabled(FEATURE_NAME);
    }
}
