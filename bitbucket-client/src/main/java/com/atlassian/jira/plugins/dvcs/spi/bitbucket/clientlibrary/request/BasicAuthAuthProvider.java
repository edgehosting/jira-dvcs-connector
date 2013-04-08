package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

public class BasicAuthAuthProvider extends AbstractAuthProvider
{
	private final String username;
	private final String password;

	public BasicAuthAuthProvider(String hostUrl, String username, String password)
	{
		super(hostUrl);
		this.username = username;
		this.password = password;
	}

	@Override
	public RemoteRequestor provideRequestor()
	{
		return new BasicAuthRemoteRequestor(this, username, password);
	}

}

