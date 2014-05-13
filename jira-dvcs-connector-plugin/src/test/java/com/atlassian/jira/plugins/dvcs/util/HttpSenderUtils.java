package com.atlassian.jira.plugins.dvcs.util;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpSenderUtils
{
    public static String makeHttpRequest(HttpMethod method, String username, String password)
    {
        try
        {
            HttpClient httpClient = new HttpClient();

            AuthScope authScope = new AuthScope(method.getURI().getHost(), AuthScope.ANY_PORT, null,
                    AuthScope.ANY_SCHEME);
            httpClient.getParams().setAuthenticationPreemptive(true);
            httpClient.getState().setCredentials(authScope, new UsernamePasswordCredentials(username, password));

            httpClient.executeMethod(method);
            return method.getResponseBodyAsString();
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void removeJsonElementsUsingIDs(String url, String username, String password)
    {
        String hooksJsonString = HttpSenderUtils.makeHttpRequest(new GetMethod(url), username, password);

        String regexp = "\"id\":\\s?([0-9]*)";
        Matcher m = Pattern.compile(regexp).matcher(hooksJsonString);
        while (m.find())
        {
            String id = m.group(1);
            // remove post-commit hook
            HttpSenderUtils.makeHttpRequest(new DeleteMethod(url+"/"+id), username, password);
        }
    }

}
