package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

import java.io.IOException;
import java.util.Map;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.BasicCredentialsProvider;
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
	    CredentialsProvider credsProvider = new BasicCredentialsProvider();
	    credsProvider.setCredentials(
	        new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), 
	        new UsernamePasswordCredentials(username, password));
	    client.setCredentialsProvider(credsProvider);
	}
}
