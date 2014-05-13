package org.scribe.model;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.util.SystemUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.scribe.exceptions.OAuthException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

public class HttpClientOauthResponse
{
    private int statusCode;
    private String content;

    public HttpClientOauthResponse (HttpResponse response, HttpRequestBase request)
    {
        // status
        try
        {
            statusCode = response.getStatusLine().getStatusCode();
            
            // content
            InputStream remoteContent = response.getEntity().getContent();
            StringWriter output = new StringWriter();
            IOUtils.copy(new BufferedReader(new InputStreamReader(remoteContent)), output);
            
            content = output.toString();
            
            SystemUtils.releaseConnection(request, response);
        
        } catch (Exception e)
        {
            throw new OAuthException("Failed to read response: " + e.getMessage(), e);
        }  
    }

    public int getStatusCode()
    {
        return statusCode;
    }

    public String getContent()
    {
        return content;
    }
    
}

