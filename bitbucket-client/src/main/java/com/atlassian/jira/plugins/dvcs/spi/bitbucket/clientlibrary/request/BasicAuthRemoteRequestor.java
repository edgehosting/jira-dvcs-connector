package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.util.SystemUtils;

/**
 * BasicAuthRemoteRequestor
 *
 *
 * <br />
 * <br />
 * Created on 13.7.2012, 10:26:31 <br />
 * <br />
 *
 * @author jhocman@atlassian.com
 *
 */
public class BasicAuthRemoteRequestor extends BaseRemoteRequestor
{

	private final String username;

	private final String password;

	public BasicAuthRemoteRequestor(String apiUrl, String username, String password)
	{
		super(apiUrl);
		this.username = username;
		this.password = password;
	}

	@Override
	protected void onConnectionCreated(HttpURLConnection connection, HttpMethod method, Map<String, String> params) throws IOException
	{
		connection.setRequestProperty(
				"Authorization",
				"Basic " + SystemUtils.encodeUsingBase64(username + ":" + password)
								.replaceAll("\n", "").replaceAll("\r", ""));
	}
}
