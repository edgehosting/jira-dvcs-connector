package com.atlassian.jira.plugins.dvcs.analytics;

/**
 * Analytics event to indicate that an add organization process has ended.
 *
 * @since v6.1
 */
public class DvcsConfigAddEndedAnalyticsEvent extends DvcsConfigAddLifecycleAnalyticsEvent
{
    public final static String OUTCOME_SUCCEEDED = "succeeded";
    public final static String OUTCOME_FAILED = "failed";

    public final static String FAILED_REASON_OAUTH_GENERIC = "oauth.generic";
    public final static String FAILED_REASON_OAUTH_SOURCECONTROL = "oauth.sourcecontrol";
    public final static String FAILED_REASON_OAUTH_RESPONSE = "oauth.response";
    public final static String FAILED_REASON_OAUTH_TOKEN = "oauth.token";
    public final static String FAILED_REASON_OAUTH_UNAUTH = "oauth.unauth";
    public final static String FAILED_REASON_VALIDATION = "validation";

    private final String reason;

    public DvcsConfigAddEndedAnalyticsEvent(String source, String type, String outcome, String reason)
    {
        super(source, "ended", type, outcome);
        this.reason = reason;
    }

    public String getReason()
    {
        return reason;
    }
}
