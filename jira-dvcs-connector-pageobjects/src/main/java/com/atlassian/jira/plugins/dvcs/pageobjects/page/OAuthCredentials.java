package com.atlassian.jira.plugins.dvcs.pageobjects.page;

public final class OAuthCredentials
{
    public final String key;
    public final String secret;

    public OAuthCredentials(String key, String secret)
    {
        this.key = key;
        this.secret = secret;
    }
}