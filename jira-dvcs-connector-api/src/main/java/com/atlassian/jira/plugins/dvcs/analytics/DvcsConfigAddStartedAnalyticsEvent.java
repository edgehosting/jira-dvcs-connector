package com.atlassian.jira.plugins.dvcs.analytics;

/**
 * Analytics event to indicate that an add organization process has started.
 */
public class DvcsConfigAddStartedAnalyticsEvent extends DvcsConfigAddLifecycleAnalyticsEvent
{
    public DvcsConfigAddStartedAnalyticsEvent(String source, String type)
    {
        super(source, "started", type);
    }
}
