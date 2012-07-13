package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

public class BasicAuthAuthProvider implements AuthProvider
{

	private final String username;
	
	private final String password;

	private final String apiUrl;

	public BasicAuthAuthProvider(String apiUrl, String username, String password)
	{
		super();
		this.apiUrl = apiUrl;
		this.username = username;
		this.password = password;
	}

	@Override
	public AuthKind getKind()
	{
		return AuthKind.BASIC_AUTH;
	}

	@Override
	public RemoteRequestor provideRequestor()
	{
		return new BasicAuthRemoteRequestor(apiUrl, username, password);
	}
	
	

}

