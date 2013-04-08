package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

public interface ApiProvider
{
    String getHostUrl();

    String getApiUrl();

    void setApiVersion(int apiVersion);

    void setUserAgent(String userAgent);

    String getUserAgent();
}
