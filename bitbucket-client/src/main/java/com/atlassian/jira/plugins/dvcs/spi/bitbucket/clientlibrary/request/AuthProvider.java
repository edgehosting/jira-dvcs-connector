package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

public interface AuthProvider
{

	public enum AuthKind {
		TWO_LEGGED_OAUTH_10a,
		THREE_LEGGED_OAUTH_10a,
		BASIC_AUTH;
	}
	
	AuthKind getKind();

	RemoteRequestor provideRequestor();
	
}

