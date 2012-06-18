package com.atlassian.jira.plugins.dvcs.auth;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;

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
        
        @Override
        public void addAuthentication(HttpMethod forMethod, HttpClient forClient) {
        	// TODO Auto-generated method stub
        	
        }
    };

    public void addAuthentication(Request<?, ?> request, String url);

    public void addAuthentication(HttpMethod forMethod, HttpClient forClient);
}
