package com.atlassian.jira.plugins.dvcs.auth;

import com.atlassian.sal.api.net.Request;

/**
 * Authentication method for accessing repository
 */
public interface Authentication
{
    /**
     * Access with no authentication details set.
     */
    public static final Authentication ANONYMOUS = new Authentication()
    {
        @Override
        public void addAuthentication(Request<?,?> request, String url)
        {
            // add no authentication headers
        }
    };

    public void addAuthentication(Request<?, ?> request, String url);
}
