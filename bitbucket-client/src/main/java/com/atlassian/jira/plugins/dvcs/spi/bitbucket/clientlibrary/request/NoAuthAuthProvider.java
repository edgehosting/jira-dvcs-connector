package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

/**
 * NoAuthAuthProvider
 *
 * 
 * <br /><br />
 * Created on 13.7.2012, 17:17:14
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class NoAuthAuthProvider extends AbstractOauthProvider
{

	public NoAuthAuthProvider(String hostUrl)
	{
		super(hostUrl);
	}

	@Override
	public RemoteRequestor provideRequestor()
	{
		return new BaseRemoteRequestor(getApiUrl());
	}


}

