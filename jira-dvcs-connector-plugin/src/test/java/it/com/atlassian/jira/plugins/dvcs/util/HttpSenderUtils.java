package it.com.atlassian.jira.plugins.dvcs.util;


import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;


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
