package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

public interface OAuthProvider
{

	public enum OAuthKind {
		TWO_LEGGED_10a,
		THREE_LEGGED_10a;
	}
	
	OAuthKind getKind();
	
	RemoteRequestor provideRequestor();
	
}

