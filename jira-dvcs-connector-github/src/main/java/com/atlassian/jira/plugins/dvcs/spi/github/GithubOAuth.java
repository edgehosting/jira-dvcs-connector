package com.atlassian.jira.plugins.dvcs.spi.github;

public interface GithubOAuth
{
    void setClient(String host, String clientID, String clientSecret);

    String getHost();

    String getClientId();

    String getClientSecret();
}
