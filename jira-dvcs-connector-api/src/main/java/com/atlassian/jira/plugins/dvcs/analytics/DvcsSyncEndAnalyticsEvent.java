package com.atlassian.jira.plugins.dvcs.analytics;

import java.util.Date;

import com.atlassian.analytics.api.annotations.EventName;

public class DvcsSyncEndAnalyticsEvent
{
    private boolean soft;
    private boolean commits;
    private boolean pullrequests;
    private boolean webhook;
    private Date finishedOn;
    private long tookMillis;

    public DvcsSyncEndAnalyticsEvent()
    {
    }

    public DvcsSyncEndAnalyticsEvent(boolean soft, boolean commits, boolean pullrequests, boolean webhook, Date finishedOn, long tookMillis)
    {
        super();
        this.soft = soft;
        this.commits = commits;
        this.pullrequests = pullrequests;
        this.webhook = webhook;
        this.finishedOn = finishedOn;
        this.tookMillis = tookMillis;
    }

    @EventName
    public String determineEventName()
    {
        return "jira.dvcsconnector.sync.end";
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

    public Date getFinishedOn()
    {
        return finishedOn;
    }

    public void setFinishedOn(Date finishedOn)
    {
        this.finishedOn = finishedOn;
    }

    public long getTookMillis()
    {
        return tookMillis;
    }

    public void setTookMillis(long tookMillis)
    {
        this.tookMillis = tookMillis;
    }

}
