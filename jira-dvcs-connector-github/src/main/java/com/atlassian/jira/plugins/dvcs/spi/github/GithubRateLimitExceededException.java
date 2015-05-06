package com.atlassian.jira.plugins.dvcs.spi.github;

import com.google.common.base.Preconditions;

public class GithubRateLimitExceededException extends RuntimeException
{
    private RateLimit rateLimit;

    public GithubRateLimitExceededException(final RateLimit rateLimit)
    {
        this.rateLimit = Preconditions.checkNotNull(rateLimit);
    }

    public RateLimit getRateLimit()
    {
        return rateLimit;
    }

    @Override
    public String getMessage()
    {
        return String.format("Github rate limit exceeded, requests: %d, remaining requests: %d, reset time: %s",
                rateLimit.getRateLimitRequest(), rateLimit.getRemainingRequests(), rateLimit.getRateLimitReset(),
                rateLimit.getRateLimitReset().toString());
    }

    @Override
    public String toString()
    {
        return getMessage();
    }
}
