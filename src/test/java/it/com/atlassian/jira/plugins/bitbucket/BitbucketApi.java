package it.com.atlassian.jira.plugins.bitbucket;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;

public class BitbucketApi
{

	public String getServices(String url, String username, String password) throws Exception
	{
		HttpClient httpClient = new HttpClient();
		HttpMethod method = new GetMethod(url);

		AuthScope authScope = new AuthScope(method.getURI().getHost(), AuthScope.ANY_PORT, null, AuthScope.ANY_SCHEME);
		httpClient.getParams().setAuthenticationPreemptive(true);
		httpClient.getState().setCredentials(authScope, new UsernamePasswordCredentials(username, password));

		httpClient.executeMethod(method);
		return method.getResponseBodyAsString();
	}

}
