package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.plugins.bitbucket.bitbucket.impl.BasicAuthentication;
import com.atlassian.sal.api.net.Request;
import org.apache.commons.lang.StringUtils;

/**
 * Authentication mode for bitbucket
 */
public abstract class BitbucketAuthentication
{
    public static final BitbucketAuthentication ANONYMOUS = new BitbucketAuthentication()
    {
        public void addAuthentication(Request<?,?> request)
        {
            // do nothing
        }
    };

    public static final BitbucketAuthentication basic(final String username, final String password)
    {
        return new BasicAuthentication(username, password);
    }

    public abstract void addAuthentication(Request<?,?> request);
}
