package com.atlassian.jira.plugins.dvcs.analytics;

import com.atlassian.analytics.api.annotations.EventName;

public class DvcsSyncStartAnalyticsEvent
{
    private boolean soft;
    private boolean commits;
    private boolean pullrequests;
    private boolean webhook;

    public DvcsSyncStartAnalyticsEvent()
    {
    }

    public DvcsSyncStartAnalyticsEvent(boolean soft, boolean commits, boolean pullrequests, boolean webhook)
    {
        super();
        this.soft = soft;
        this.commits = commits;
        this.pullrequests = pullrequests;
        this.webhook = webhook;
    }

    @EventName
    public String determineEventName()
    {
        return "jira.dvcsconnector.sync.start";
    }

    public boolean isSoft()
    {
        return soft;
    }

    public void setSoft(boolean soft)
    {
        this.soft = soft;
    }

    public boolean isCommits()
    {
        return commits;
    }

    public void setCommits(boolean commits)
    {
        this.commits = commits;
    }

    public boolean isPullrequests()
    {
        return pullrequests;
    }

    public void setPullrequests(boolean pullrequests)
    {
        this.pullrequests = pullrequests;
    }

    public boolean isWebhook()
    {
        return webhook;
    }

    public void setWebhook(boolean webhook)
    {
        this.webhook = webhook;
    }

}
