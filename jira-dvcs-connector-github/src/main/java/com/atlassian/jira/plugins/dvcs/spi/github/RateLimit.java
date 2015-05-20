package com.atlassian.jira.plugins.dvcs.spi.github;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

public class RateLimit
{
    private int rateLimitRequest;
    private int remainingRequests;
    private DateTime rateLimitReset;

    public RateLimit(final int rateLimitRequest, final int remainingRequests, final long rateLimitReset)
    {
        this.rateLimitRequest = rateLimitRequest;
        this.remainingRequests = remainingRequests;
        this.rateLimitReset = new DateTime(rateLimitReset * 1000L, DateTimeZone.UTC).withZone(DateTimeZone.getDefault());
    }

    public DateTime getRateLimitReset()
    {
        return rateLimitReset;
    }

    public int getRateLimitRequest()
    {
        return rateLimitRequest;
    }

    public int getRemainingRequests()
    {
        return remainingRequests;
    }

    public long getRateLimitResetInMinutes(final DateTime currentTime)
    {
        Duration duration = new Duration(currentTime, rateLimitReset);
        return Math.max(1, duration.getStandardMinutes());
    }
}
