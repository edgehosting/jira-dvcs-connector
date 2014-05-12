package com.atlassian.jira.plugins.dvcs.auth.impl;

import com.atlassian.jira.plugins.dvcs.auth.Authentication;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.sal.api.net.Request;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
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
	public void addAuthentication(HttpMethod forMethod, HttpClient client) {
    	
		forMethod.getParams().setCredentialCharset("utf-8");
	
		try {
			
			AuthScope authscope = new AuthScope(forMethod.getURI().getHost(),
					AuthScope.ANY_PORT, null, AuthScope.ANY_SCHEME);
			client.getState().setCredentials(authscope,
					new UsernamePasswordCredentials(username, password));
			client.getParams().setAuthenticationPreemptive(true);
			
		} catch (URIException uriException) {
			throw new SourceControlException("Decoding of given URI has failed. " + uriException.getMessage(), uriException);
		}
    	
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
