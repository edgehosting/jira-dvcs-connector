package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AbstractOauthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;

/**
 * ThreeLegged10aOauthProvider
 *
 * 
 * <br /><br />
 * Created on 13.7.2012, 10:25:48
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class ThreeLegged10aOauthProvider extends AbstractOauthProvider
{

	private final String accessTokenWithSecret;
    private final String key;
    private final String secret;

	public ThreeLegged10aOauthProvider(String hostUrl, String key, String secret, String accessTokenWithSecret)
	{
		super(hostUrl);
        this.key = key;
        this.secret = secret;
		this.accessTokenWithSecret = accessTokenWithSecret;
	}

	@Override
	public RemoteRequestor provideRequestor()
	{
		return new ThreeLegged10aOauthRemoteRequestor(getApiUrl(), key, secret, accessTokenWithSecret);
	}

}

