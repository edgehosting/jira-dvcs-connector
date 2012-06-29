package com.atlassian.jira.plugins.dvcs.util;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;


/**
 * @author Martin Skurla
 */
public final class HttpSenderUtils {

    private HttpSenderUtils() {}


    public static String sendGetHttpRequest(String url, String username, String password) throws IOException
    {
		HttpClient httpClient = new HttpClient();
		HttpMethod method = new GetMethod(url);

		AuthScope authScope = new AuthScope(method.getURI().getHost(),
                                            AuthScope.ANY_PORT,
                                            null,
                                            AuthScope.ANY_SCHEME);

        httpClient.getParams().setAuthenticationPreemptive(true);
		httpClient.getState().setCredentials(authScope,
                                             new UsernamePasswordCredentials(username, password));

		httpClient.executeMethod(method);
		return method.getResponseBodyAsString();
    }
    
    public static String sendPostHttpRequest(String url, Map<String, String> params) throws IOException
    {
		HttpClient httpClient = new HttpClient();
		PostMethod method = new PostMethod(url);
		method.addParameters(toPairs(params));
		httpClient.executeMethod(method);
		
		return method.getResponseBodyAsString();
    }

	private static NameValuePair [] toPairs(Map<String, String> params)
	{
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		for (String key : params.keySet())
		{
			pairs.add(new NameValuePair(key, params.get(key)));
		}
		
		return pairs.toArray(new NameValuePair[]{});
	}


	public static void sendDeleteHttpRequest(String url, String username, String password) throws IOException
	{
		HttpClient httpClient = new HttpClient();
		HttpMethod method = new DeleteMethod(url);

		AuthScope authScope = new AuthScope(method.getURI().getHost(),
                                            AuthScope.ANY_PORT,
                                            null,
                                            AuthScope.ANY_SCHEME);

        httpClient.getParams().setAuthenticationPreemptive(true);
		httpClient.getState().setCredentials(authScope,
                                             new UsernamePasswordCredentials(username, password));

		httpClient.executeMethod(method);
	}
}
