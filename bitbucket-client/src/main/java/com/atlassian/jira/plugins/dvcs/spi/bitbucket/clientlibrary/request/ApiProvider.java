package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

public interface ApiProvider
{
    String getApiUrl();
    void setApiVersion(int apiVersion);
    String getHostUrl();
    void setUserAgent(String userAgent);
    String getUserAgent();
}
