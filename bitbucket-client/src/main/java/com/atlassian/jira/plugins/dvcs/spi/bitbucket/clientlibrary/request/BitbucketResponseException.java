package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

/**
 * RequestFailedException
 *
 * 
 * <br /><br />
 * Created on 12.7.2012, 17:18:48
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class BitbucketResponseException extends RuntimeException
{

	private static final long serialVersionUID = 2834085757547295408L;

    public BitbucketResponseException() {}
    
	public BitbucketResponseException(String message)
	{
		super(message);
	}

	public BitbucketResponseException(Throwable cause)
	{
		super(cause);
	}

	public BitbucketResponseException(String message, Throwable cause)
	{
		super(message, cause);
	}

    
    public static final class Unauthorized_401 extends BitbucketResponseException
    {
    }
    
    public static final class Forbidden_403 extends BitbucketResponseException
    {
    }
    
    public static final class NotFound_404 extends BitbucketResponseException
    {
    }
}

