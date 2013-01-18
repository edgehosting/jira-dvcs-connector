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

	
	public static final class BadRequest_400 extends BitbucketRequestException
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
    }
}

