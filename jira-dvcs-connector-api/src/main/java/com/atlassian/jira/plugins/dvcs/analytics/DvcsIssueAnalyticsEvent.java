package com.atlassian.jira.plugins.dvcs.analytics;

import com.atlassian.analytics.api.annotations.EventName;

/**
 * Analytics event class to indicate actions on the issue.
 */
public class DvcsIssueAnalyticsEvent
{

    private final String eventName;
    private final boolean isAuthenticated;
    private final String source;

    public DvcsIssueAnalyticsEvent(final String source, final String eventName, final boolean isAuthenticated)
    {
        this.eventName = eventName;
        this.isAuthenticated = isAuthenticated;
        this.source = source;
    }

    @EventName
    public String determineEventName()
    {
        return "jira.dvcsconnector.issue." + eventName;
    }

    public boolean isAuthenticated()
    {
        return isAuthenticated;
    }

    public String getSource()
    {
        return source;
    }
}
