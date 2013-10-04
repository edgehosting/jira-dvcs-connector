package com.atlassian.jira.plugins.dvcs.analytics;

import com.atlassian.analytics.api.annotations.EventName;

/**
 * Analytics event class to indicate actions on the issue.
 *
 * Possible events are 'tabclick' when the 'Commit' tab is clicked and 'tabshowing' when the
 * 'Commit' tab is rendered and showing.
 * Possible sources are 'issue' for tab in issue and 'agile' for Jira Agile tab.
 */
public class DvcsCommitsAnalyticsEvent
{

    private final String eventName;
    private final boolean isAuthenticated;
    private final String source;

    public DvcsCommitsAnalyticsEvent(final String source, final String eventName, final boolean isAuthenticated)
    {
        this.eventName = eventName;
        this.isAuthenticated = isAuthenticated;
        this.source = source;
    }

    @EventName
    public String determineEventName()
    {
        return "jira.dvcsconnector.commit." + eventName;
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
