package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AbstractAuthProvider;
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
public class TwoLeggedOauthProvider extends AbstractAuthProvider
{
	private final String key;

	private final String secret;

	public TwoLeggedOauthProvider(String hostUrl, String key, String secret)
	{
		super(hostUrl);
		this.key = key;
		this.secret = secret;
	}

	@Override
	public RemoteRequestor provideRequestor()
	{
		return new TwoLegged10aOauthRemoteRequestor(this, key, secret);
	}


}

