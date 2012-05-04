package com.atlassian.jira.plugins.dvcs.spi.github;

public interface GithubOAuth
{
    void setClient(String clientID, String clientSecret);

    String getClientId();

    String getClientSecret();
}
