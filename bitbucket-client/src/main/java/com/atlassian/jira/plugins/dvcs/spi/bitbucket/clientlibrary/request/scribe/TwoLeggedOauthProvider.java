package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;

/**
 * TwoLeggedOauthProvider
 *
 * 
 * <br /><br />
 * Created on 13.7.2012, 15:06:37
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class TwoLeggedOauthProvider implements AuthProvider
{

	private final String key;

	private final String secret;

	private final String apiUrl;

	public TwoLeggedOauthProvider(String apiUrl, String key, String secret)
	{
		super();
		this.apiUrl = apiUrl;
		this.key = key;
		this.secret = secret;
	}

	@Override
	public AuthKind getKind()
	{
		return AuthKind.TWO_LEGGED_OAUTH_10a;
	}

	@Override
	public RemoteRequestor provideRequestor()
	{
		return new TwoLeggedOauthRemoteRequestor(apiUrl, key, secret);
	}


}

