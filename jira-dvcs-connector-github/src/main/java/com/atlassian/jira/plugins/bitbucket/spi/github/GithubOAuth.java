package com.atlassian.jira.plugins.bitbucket.spi.github;

public interface GithubOAuth
{
    void setClient(String clientID, String clientSecret);

    String getClientId();

    String getClientSecret();
}
