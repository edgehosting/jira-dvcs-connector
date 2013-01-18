package com.atlassian.jira.plugins.dvcs.spi.github;

public interface GithubOAuth
{
    void setClient(String clientID, String clientSecret);

    String getClientId();

    String getClientSecret();
    
    // enterprise credentials
    
    void setEnterpriseClient(String hostUrl, String clientID, String clientSecret);

    String getEnterpriseClientId();

    String getEnterpriseClientSecret();
    
    String getEnterpriseHostUrl();
}
