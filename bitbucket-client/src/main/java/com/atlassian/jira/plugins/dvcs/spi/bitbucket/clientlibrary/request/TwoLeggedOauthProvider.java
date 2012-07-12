package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

public class TwoLeggedOauthProvider implements OAuthProvider
{

	private final String key;
	
	private final String secret;

	public TwoLeggedOauthProvider(String key, String secret)
	{
		super();
		this.key = key;
		this.secret = secret;
	}


	@Override
	public OAuthKind getKind()
	{
		return OAuthKind.TWO_LEGGED_10a;
	}

	@Override
	public RemoteRequestor provideRequestor()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	

}

