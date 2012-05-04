package com.atlassian.jira.plugins.dvcs.auth;

import com.atlassian.sal.api.net.Request;
import org.apache.commons.lang.StringUtils;

/**
 * Basic authentication
 */
public class BasicAuthentication implements Authentication
{
    private final String username;
    private final String password;

    public BasicAuthentication(String username, String password)
    {
        this.username = username;
        this.password = password;
    }

    @Override
    public void addAuthentication(Request<?, ?> request, String url)
    {
        // add basic authentication
        if (!StringUtils.isBlank(username) && !StringUtils.isBlank(password))
            request.addBasicAuthentication(username, password);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BasicAuthentication that = (BasicAuthentication) o;
        if (password != null ? !password.equals(that.password) : that.password != null) return false;
        if (username != null ? !username.equals(that.username) : that.username != null) return false;
        return true;
    }

    @Override
    public int hashCode()
    {
        int result = username != null ? username.hashCode() : 0;
        result = 31 * result + (password != null ? password.hashCode() : 0);
        return result;
    }
}
