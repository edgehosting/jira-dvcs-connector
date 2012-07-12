package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

import java.io.InputStream;
import java.io.Serializable;

public class RemoteResponse implements Serializable
{

	private static final long serialVersionUID = -8160018795770610703L;

	private InputStream response;

	private int httpStatusCode;

	public RemoteResponse()
	{
		super();
	}

	public InputStream getResponse()
	{
		return response;
	}

	public void setResponse(InputStream response)
	{
		this.response = response;
	}

	public int getHttpStatusCode()
	{
		return httpStatusCode;
	}

	public void setHttpStatusCode(int httpStatusCode)
	{
		this.httpStatusCode = httpStatusCode;
	}

}

