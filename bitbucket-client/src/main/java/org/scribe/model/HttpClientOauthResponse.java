package org.scribe.model;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.scribe.exceptions.OAuthException;

public class HttpClientOauthResponse
{
    private int statusCode;
    private String content;

    public HttpClientOauthResponse (HttpResponse response)
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

