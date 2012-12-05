package com.atlassian.jira.plugins.dvcs.spi.github;

public interface GithubOauthProvider
{

    String provideClientId();

    String provideClientSecret();
    
}

