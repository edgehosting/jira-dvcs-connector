package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

public interface AuthProvider
{

	RemoteRequestor provideRequestor();
	
	String getApiUrl();
	
}

