package com.atlassian.jira.plugins.dvcs.analytics;

/**
 * Analytics event to indicate that an add organization process has entered into a stage in the lifecycle.
 *
 * @since v6.1
 */
public class DvcsConfigAddLifecycleAnalyticsEvent extends DvcsConfigAnalyticsEvent
{
    public DvcsConfigAddLifecycleAnalyticsEvent(String source, String stage, String type)
    {
        super(source, "add." + type + "." + stage);
    }

    public DvcsConfigAddLifecycleAnalyticsEvent(String source, String stage, String type, String extra)
    {
        super(source, "add." + type + "." + stage + "." + extra);
    }
}
