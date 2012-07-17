package com.atlassian.jira.plugins.dvcs.spi.bitbucket;

public interface BitbucketOAuth
{
    void setClient(String clientID, String clientSecret);

    String getClientId();

    String getClientSecret();
}
