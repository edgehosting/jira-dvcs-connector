package com.atlassian.jira.plugins.dvcs.spi.github;

public interface GithubOAuth
{
    void setClient(String hostUrl, String clientID, String clientSecret);

    String getClientId();

    String getClientSecret();

    String getHostUrl();
    
}
