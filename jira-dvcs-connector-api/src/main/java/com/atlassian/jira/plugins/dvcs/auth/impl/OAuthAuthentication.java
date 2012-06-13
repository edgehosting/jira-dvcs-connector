package com.atlassian.jira.plugins.dvcs.auth.impl;

import com.atlassian.jira.plugins.dvcs.auth.Authentication;
import com.atlassian.sal.api.net.Request;

public class OAuthAuthentication implements Authentication
{

    private final String accessToken;

    public OAuthAuthentication(String accessToken)
    {
        this.accessToken = accessToken;
    }

    @Override
    public void addAuthentication(Request<?, ?> request, String url)
    {
        request.addHeader("Authorization", "token "+accessToken);
        
        String separator = url.contains("?") ? "&" : "?";
        url += separator + "access_token=" + getAccessToken();
        request.setUrl(url);
    }

    public String getAccessToken()
    {
        return accessToken;
    }
    
    
}
