package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * ThreeLeggedOauthRemoteRequestor
 *
 * 
 * <br /><br />
 * Created on 13.7.2012, 10:26:08
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class ThreeLeggedOauthRemoteRequestor extends BaseRemoteRequestor
{

	private final String accessToken;

	public ThreeLeggedOauthRemoteRequestor(String apiUrl, String accessToken)
	{
		super(apiUrl);
		this.accessToken = accessToken;
	}
	
	@Override
	protected void onConnectionCreated(HttpURLConnection connection, HttpMethod method) throws IOException
	{
		connection.addRequestProperty("access_token", accessToken);
	}
}

