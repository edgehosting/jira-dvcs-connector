package com.atlassian.jira.plugins.bitbucket.spi.bitbucket;

/**
 * Indicates an unhandled exceptional condition within the bitbucket client
 */
public class BitbucketException extends RuntimeException
{
    public BitbucketException()
    {
    }

    public BitbucketException(String message)
    {
        super(message);
    }

    public BitbucketException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public BitbucketException(Throwable cause)
    {
        super(cause);
    }
}
