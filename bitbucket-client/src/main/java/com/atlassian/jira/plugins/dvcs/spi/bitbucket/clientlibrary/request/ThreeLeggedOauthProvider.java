package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

public class ThreeLeggedOauthProvider implements OAuthProvider
{

	private final String accessToken;
	
	public ThreeLeggedOauthProvider(String accessToken)
	{
		super();
		this.accessToken = accessToken;
	}

	@Override
	public OAuthKind getKind()
	{
		return OAuthKind.THREE_LEGGED_10a;
	}

	@Override
	public RemoteRequestor provideRequestor()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	

}

