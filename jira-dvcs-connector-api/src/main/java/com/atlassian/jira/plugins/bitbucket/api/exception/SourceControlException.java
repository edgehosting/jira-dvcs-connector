package com.atlassian.jira.plugins.bitbucket.api.exception;

/**
 * Indicates an unhandled exceptional condition within the bitbucket client
 */
public class SourceControlException extends RuntimeException
{
    public SourceControlException()
    {
    }

    public SourceControlException(String message)
    {
        super(message);
    }

    public SourceControlException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public SourceControlException(Throwable cause)
    {
        super(cause);
    }
    
    
    public static class UnauthorisedException extends SourceControlException
    {
        public UnauthorisedException(String message)
        {
            super(message);
        }
        
    }
}
