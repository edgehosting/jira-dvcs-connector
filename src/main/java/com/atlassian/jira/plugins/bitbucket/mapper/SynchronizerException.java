package com.atlassian.jira.plugins.bitbucket.mapper;

/**
 * Indicates an exceptional case during the synchronization
 */
public class SynchronizerException extends RuntimeException
{
    public SynchronizerException()
    {
    }

    public SynchronizerException(String message)
    {
        super(message);
    }

    public SynchronizerException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public SynchronizerException(Throwable cause)
    {
        super(cause);
    }
}
