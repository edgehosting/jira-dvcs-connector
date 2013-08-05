package com.atlassian.jira.plugins.dvcs.analytics;

/**
 * An event to indicate that the ConfigureDvcsOrganizations page is shown.
 *
 * @since v6.1
 */
public class DvcsConfigPageShownAnalyticsEvent extends DvcsConfigAnalyticsEvent
{
    public DvcsConfigPageShownAnalyticsEvent(String source)
    {
        super(source, "shown");
    }
}
