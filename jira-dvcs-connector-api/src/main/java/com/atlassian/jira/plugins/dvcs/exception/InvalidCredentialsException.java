package com.atlassian.jira.plugins.dvcs.exception;

/**
 * 
 * Thrown when trying to set up new account with invalid
 * credentials (basic auth, oauth, etc, ...)
 *
 * 
 * <br /><br />
 * Created on 18.5.2012, 16:34:31
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class InvalidCredentialsException extends RuntimeException
{

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -5203187762236105970L;

	/**
	 * The Constructor.
	 */
	public InvalidCredentialsException()
	{
		super();
	}

	/**
	 * The Constructor.
	 *
	 * @param message the message
	 */
	public InvalidCredentialsException(String message)
	{
		super(message);
	}

	/**
	 * The Constructor.
	 *
	 * @param cause the cause
	 */
	public InvalidCredentialsException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * The Constructor.
	 *
	 * @param message the message
	 * @param cause the cause
	 */
	public InvalidCredentialsException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
