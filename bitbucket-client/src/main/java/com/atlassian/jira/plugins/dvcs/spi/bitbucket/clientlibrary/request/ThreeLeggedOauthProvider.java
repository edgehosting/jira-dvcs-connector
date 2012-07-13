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
public class ThreeLeggedOauthProvider implements AuthProvider
{

	private final String accessToken;

	private final String apiUrl;
	
	public ThreeLeggedOauthProvider(String apiUrl, String accessToken)
	{
		super();
		this.apiUrl = apiUrl;
		this.accessToken = accessToken;
	}

	@Override
	public AuthKind getKind()
	{
		return AuthKind.THREE_LEGGED_OAUTH_10a;
	}

	@Override
	public RemoteRequestor provideRequestor()
	{
		return new ThreeLeggedOauthRemoteRequestor(apiUrl, accessToken);
	}
	
	

}

