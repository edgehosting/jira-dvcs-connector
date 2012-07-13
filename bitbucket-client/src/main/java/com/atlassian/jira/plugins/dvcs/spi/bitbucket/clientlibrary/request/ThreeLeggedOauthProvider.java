package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

/**
 * ThreeLeggedOauthProvider
 *
 * 
 * <br /><br />
 * Created on 13.7.2012, 10:25:48
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class ThreeLeggedOauthProvider extends AbstractOauthProvider
{

	private final String accessToken;

	public ThreeLeggedOauthProvider(String hostUrl, String accessToken)
	{
		super(hostUrl);
		this.accessToken = accessToken;
	}

	@Override
	public RemoteRequestor provideRequestor()
	{
		return new ThreeLeggedOauthRemoteRequestor(getApiUrl(), accessToken);
	}

}

