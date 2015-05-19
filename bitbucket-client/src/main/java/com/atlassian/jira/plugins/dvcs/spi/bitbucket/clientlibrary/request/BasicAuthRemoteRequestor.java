package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.auth.BasicScheme;

import java.io.IOException;
import java.util.Map;

public class BasicAuthRemoteRequestor extends BaseRemoteRequestor
{

	private final String username;

	private final String password;

	public BasicAuthRemoteRequestor(ApiProvider apiProvider, String username, String password, HttpClientProvider httpClientProvider)
	{
		super(apiProvider, httpClientProvider);
		this.username = username;
		this.password = password;
	}

	@Override
	protected void onConnectionCreated(HttpClient client, HttpRequestBase method, Map<String, ? extends Object> params)
	        throws IOException
	{
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials(username, password);
        try
        {
            method.addHeader(new BasicScheme().authenticate(creds, method));
        } catch (AuthenticationException e)
        {
            // This should not happen for BasicScheme
        }
	}
}
