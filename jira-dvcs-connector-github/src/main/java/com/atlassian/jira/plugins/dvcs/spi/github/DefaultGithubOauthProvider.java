package com.atlassian.jira.plugins.dvcs.spi.github;

public class DefaultGithubOauthProvider implements GithubOauthProvider
{

    private final boolean isGithubEnterprise;
    private final GithubOAuth settings;

    private DefaultGithubOauthProvider(GithubOAuth settings, boolean isGithubEnterprise) 
    {
        this.settings = settings;
        this.isGithubEnterprise = isGithubEnterprise;
    }
    
    @Override
    public String provideClientId()
    {
        if (isGithubEnterprise) 
        {
            return settings.getEnterpriseClientId();
        }
        return settings.getClientId();
    }

    @Override
    public String provideClientSecret()
    {
        if (isGithubEnterprise) 
        {
            return settings.getEnterpriseClientSecret();
        }
        return settings.getClientSecret();
    }

    public static GithubOauthProvider createEnterpriseProvider(GithubOAuth settings)
    {
        return new DefaultGithubOauthProvider(settings, true);
    }
    
    public static GithubOauthProvider createProvider(GithubOAuth settings)
    {
        return new DefaultGithubOauthProvider(settings, false);
    }

}

