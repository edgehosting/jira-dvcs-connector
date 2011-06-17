package com.atlassian.jira.plugins.bitbucket.bitbucket;

/**
 * An exception case indicating the remote resource was not found on the bitbucket server
 */
public class BitbucketResourceNotFoundException extends BitbucketException
{
    public BitbucketResourceNotFoundException()
    {
    }

    public BitbucketResourceNotFoundException(String message)
    {
        super(message);
    }

    public BitbucketResourceNotFoundException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public BitbucketResourceNotFoundException(Throwable cause)
    {
        super(cause);
    }
}
