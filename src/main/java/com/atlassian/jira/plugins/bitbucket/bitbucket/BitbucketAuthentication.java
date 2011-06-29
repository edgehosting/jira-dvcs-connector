package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.plugins.bitbucket.bitbucket.impl.BasicAuthentication;
import com.atlassian.sal.api.net.Request;
import org.apache.commons.lang.StringUtils;

/**
 * Authentication method for bitbucket
 */
public abstract class BitbucketAuthentication
{
    /**
     * Access bitbucket with no authentication details set.
     */
    public static final BitbucketAuthentication ANONYMOUS = new BitbucketAuthentication()
    {
        public void addAuthentication(Request<?,?> request)
        {
            // add no authentication headers
        }
    };

    /**
     * Access bitbucket with basic authentication.
     *
     * @param username the username to authenticate as
     * @param password the password to authenticate with
     * @return a basic authentication method
     */
    public static BitbucketAuthentication basic(final String username, final String password)
    {
        return new BasicAuthentication(username, password);
    }

    public abstract void addAuthentication(Request<?,?> request);
}
