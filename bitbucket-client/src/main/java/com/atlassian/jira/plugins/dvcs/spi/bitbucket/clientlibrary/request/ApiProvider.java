package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

public interface ApiProvider
{
    String getHostUrl();

    String getApiUrl();

    void setApiVersion(int apiVersion);

    void setCached(boolean cached);

    boolean isCached();
}
