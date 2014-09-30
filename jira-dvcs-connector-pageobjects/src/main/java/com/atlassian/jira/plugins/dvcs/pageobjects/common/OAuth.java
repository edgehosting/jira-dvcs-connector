package com.atlassian.jira.plugins.dvcs.pageobjects.common;


public class OAuth
{
    public final String key;
    public final String secret;
    public final String applicationId;

    public OAuth(String key, String secret, String applicationId)
    {
        this.key = key;
        this.secret = secret;
        this.applicationId = applicationId;
    }

}
