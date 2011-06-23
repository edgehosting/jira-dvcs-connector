package com.atlassian.jira.plugins.bitbucket.bitbucket.impl;

import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketAuthentication;
import com.atlassian.sal.api.net.Request;
import org.apache.commons.lang.StringUtils;

/**
 * Basic authentication
 */
public class BasicAuthentication extends BitbucketAuthentication
{
    private final String username;
    private final String password;

    public BasicAuthentication(String username, String password)
    {
        this.username = username;
        this.password = password;
    }

    public void addAuthentication(Request<?, ?> request)
    {
        // add basic authentication
        if (!StringUtils.isBlank(username) && !StringUtils.isBlank(password))
            request.addBasicAuthentication(username, password);
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }
}
