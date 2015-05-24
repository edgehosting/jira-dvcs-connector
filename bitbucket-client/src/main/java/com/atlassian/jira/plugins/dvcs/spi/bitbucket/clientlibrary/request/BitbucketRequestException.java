package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

public class BitbucketRequestException extends RuntimeException
{

    private static final long serialVersionUID = 2834085757547295408L;

	public BitbucketRequestException()
	{
		super();
	}

	public BitbucketRequestException(String message)
	{
		super(message);
	}

	public BitbucketRequestException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public static final class Other extends BitbucketRequestException implements RetryableRequestException
	{
		public Other()
		{
		}
		
		public Other(String message)
		{
			super(message);
		}
	}
	
	public static final class BadRequest_400 extends BitbucketRequestException implements RetryableRequestException
	{
	}
    
    public static final class Unauthorized_401 extends BitbucketRequestException
    {
    }
    
    public static final class Forbidden_403 extends BitbucketRequestException
    {
    }
    
    public static final class NotFound_404 extends BitbucketRequestException
    {
        public NotFound_404()
        {
            super();
        }
        public NotFound_404(String message)
        {
            super(message);
        }
        
    }

    public static final class InternalServerError_500 extends BitbucketRequestException
    {
        public InternalServerError_500()
        {
            super();
        }
        public InternalServerError_500(String message)
        {
            super(message);
        }

    }
    
    /**
     * Marker interface for {@link BitbucketRequestException}. All requests that throws an exception marked by this interface
     * will be retried
     */
    public static interface RetryableRequestException 
    {
    };
}

