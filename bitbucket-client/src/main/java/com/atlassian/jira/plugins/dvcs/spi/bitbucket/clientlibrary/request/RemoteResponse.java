package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

import java.io.InputStream;
import java.io.Serializable;

import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteResponse implements Serializable
{

    private static final long serialVersionUID = -8160018795770610703L;
    private final Logger log = LoggerFactory.getLogger(RemoteResponse.class);

    private InputStream inputStream;

    private int httpStatusCode;
    private HttpClient client;

    public RemoteResponse()
    {
        super();
    }

    public InputStream getResponse()
    {
        return inputStream;
    }

    public void setResponse(InputStream response)
    {
        this.inputStream = response;
    }

    public int getHttpStatusCode()
    {
        return httpStatusCode;
    }

    public void setHttpStatusCode(int httpStatusCode)
    {
        this.httpStatusCode = httpStatusCode;
    }

    public void close()
    {
        try
        {
            if (inputStream!=null)
            {
                try
                {
                    inputStream.close();
                } catch (Exception ignore)
                {
                    // ignore
                }
            }
            
        } finally
        {
//            client.getConnectionManager().shutdown();
        }
    }

    public void setHttpClient(HttpClient client)
    {
        this.client = client;
    }

}
