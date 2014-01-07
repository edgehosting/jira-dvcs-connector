package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

/**
 * NoAuthAuthProvider
 * 
 * Created on 13.7.2012, 17:17:14
 * 
 * @author jhocman@atlassian.com
 *
 */
public class NoAuthAuthProvider extends AbstractAuthProvider
{
	public NoAuthAuthProvider(String hostUrl, HttpClientProvider httpClientProvider)
	{
		super(hostUrl, httpClientProvider);
	}

	@Override
	public RemoteRequestor provideRequestor()
	{
		return new BaseRemoteRequestor(this, httpClientProvider);
	}
}

