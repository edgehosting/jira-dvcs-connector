package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.plugins.bitbucket.bitbucket.impl.BasicAuthentication;
import com.atlassian.sal.api.net.Request;

/**
 * Authentication method for accessing repository
 */
public abstract class Authentication
{
    /**
     * Access with no authentication details set.
     */
    public static final Authentication ANONYMOUS = new Authentication()
    {
        public void addAuthentication(Request<?,?> request)
        {
            // add no authentication headers
        }
    };

    /**
     * Access with basic authentication.
     *
     * @param username the username to authenticate as
     * @param password the password to authenticate with
     * @return a basic authentication method
     */
    public static Authentication basic(final String username, final String password)
    {
        return new BasicAuthentication(username, password);
    }

    public abstract void addAuthentication(Request<?,?> request);
}
