package com.atlassian.jira.plugins.dvcs.exception;


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
    
    public static class Forbidden_403 extends SourceControlException
    {

        public Forbidden_403(Throwable cause) 
        {
            super(cause);
        }

    }

    public static class UnauthorisedException extends SourceControlException
    {
        public UnauthorisedException(String message)
        {
            super(message);
        }
        
        public UnauthorisedException(String message, Throwable cause)
        {
            super(message, cause);
        }
        
        public UnauthorisedException(Throwable cause)
        {
            super(cause);
        }
    }
    
    public static class InvalidResponseException extends SourceControlException
    {
        public InvalidResponseException(String message)
        {
            super(message);
        }
        
        public InvalidResponseException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }
    
    public static class PostCommitHookRegistrationException extends SourceControlException
    {
        public PostCommitHookRegistrationException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }

    public static class SynchronizationDisabled extends SourceControlException
    {
        public SynchronizationDisabled(final String message)
        {
            super(message);
        }
    }
}
