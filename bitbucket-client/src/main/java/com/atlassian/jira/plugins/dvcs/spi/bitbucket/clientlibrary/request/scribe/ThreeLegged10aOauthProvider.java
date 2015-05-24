package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AbstractAuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;

/**
 * ThreeLegged10aOauthProvider
 *
 * @author jhocman@atlassian.com
 */
public class ThreeLegged10aOauthProvider extends AbstractAuthProvider
{
	private final String accessTokenWithSecret;
    private final String key;
    private final String secret;

	public ThreeLegged10aOauthProvider(String hostUrl, String key, String secret, String accessTokenWithSecret, HttpClientProvider httpClientProvider)
	{
		super(hostUrl, httpClientProvider);
        this.key = key;
        this.secret = secret;
		this.accessTokenWithSecret = accessTokenWithSecret;
	}

	@Override
	public RemoteRequestor provideRequestor()
	{
		return new ThreeLegged10aOauthRemoteRequestor(this, key, secret, accessTokenWithSecret, httpClientProvider);
	}
}

