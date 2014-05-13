package com.atlassian.jira.plugins.dvcs.auth.impl;

import com.atlassian.jira.plugins.dvcs.auth.Authentication;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.sal.api.net.Request;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;

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
        request.addHeader("Authorization", "token " + accessToken);
        
        String separator = url.contains("?") ? "&" : "?";
        url += separator + "access_token=" + getAccessToken();
        request.setUrl(url);
    }

	@Override
	public void addAuthentication(HttpMethod forMethod, HttpClient forClient) {

		try {
	
			forMethod.addRequestHeader("Authorization", "token " + accessToken);
			String url = forMethod.getURI().toString();
			
			String separator = url.contains("?") ? "&" : "?";
			url += separator + "access_token=" + getAccessToken();

			forMethod.setURI(new URI(url, true));
		
		} catch (URIException e) {
			throw new SourceControlException("Failed to decode/encode given URI. " + e.getMessage(), e);
		}

	}

    public String getAccessToken()
    {
        return accessToken;
    }
    
}
