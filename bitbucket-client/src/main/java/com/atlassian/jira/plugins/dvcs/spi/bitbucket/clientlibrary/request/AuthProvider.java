package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

public interface AuthProvider extends ApiProvider
{
    RemoteRequestor provideRequestor();
}


