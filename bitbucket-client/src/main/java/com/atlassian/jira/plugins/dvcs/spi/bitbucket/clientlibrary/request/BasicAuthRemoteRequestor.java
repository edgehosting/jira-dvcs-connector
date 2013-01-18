package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

import java.io.IOException;
import java.util.Map;

import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;

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
	protected void onConnectionCreated(DefaultHttpClient client, HttpRequestBase method, Map<String, String> params)
	        throws IOException
	{
	    UsernamePasswordCredentials creds = new UsernamePasswordCredentials(username, password);
		try {
			method.addHeader(new BasicScheme().authenticate(creds, method));
		} catch (AuthenticationException e) {
			// This should not happen for BasicScheme
		}
	}
}
