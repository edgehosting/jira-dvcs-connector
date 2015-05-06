package com.atlassian.jira.plugins.dvcs.spi.github;

import org.joda.time.DateTime;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class RateLimitTest
{
    private RateLimit rateLimit = new RateLimit(10, 1, 1430794469);
    private static final int DURATION = 15;

    @Test
    public void testGetRateLimitReset() throws Exception
    {
        assertEquals("2015-05-05T12:54:29.000+10:00", rateLimit.getRateLimitReset().toString());
    }

    @Test
    public void testGetWaitingDuration()
    {
        DateTime currentTime = rateLimit.getRateLimitReset().minusMinutes(DURATION);
        assertEquals(DURATION, rateLimit.getRateLimitResetInMinutes(currentTime));
    }

    @Test
    public void testGetRateLimitRequest() throws Exception
    {
        assertEquals(10, rateLimit.getRateLimitRequest());
    }

    @Test
    public void testGetRemainingRequests() throws Exception
    {
        assertEquals(1, rateLimit.getRemainingRequests());
    }
}