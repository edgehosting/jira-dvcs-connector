package com.atlassian.jira.plugins.bitbucket.api.impl;

import com.atlassian.jira.plugins.bitbucket.api.Authentication;
import com.atlassian.sal.api.net.Request;

public class GithubOAuthAuthentication implements Authentication
{

    private final String accessToken;

    public GithubOAuthAuthentication(String accessToken)
    {
        this.accessToken = accessToken;
    }

    @Override
    public void addAuthentication(Request<?, ?> request, String url)
    {
        request.addHeader("Authorization", "token "+accessToken);
    }

    public String getAccessToken()
    {
        return accessToken;
    }
    
    
}