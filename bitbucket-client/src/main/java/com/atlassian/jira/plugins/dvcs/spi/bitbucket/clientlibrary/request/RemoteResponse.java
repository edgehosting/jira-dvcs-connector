package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;

public class RemoteResponse implements Serializable
{

    private static final long serialVersionUID = -8160018795770610703L;

    private InputStream response;

    private int httpStatusCode;

    private HttpURLConnection connection;

    public RemoteResponse()
    {
        super();
    }

    public InputStream getResponse()
    {
        return response;
    }

    public void setResponse(InputStream response)
    {
        this.response = response;
    }

    public int getHttpStatusCode()
    {
        return httpStatusCode;
    }

    public void setHttpStatusCode(int httpStatusCode)
    {
        this.httpStatusCode = httpStatusCode;
    }

    public void setConnection(HttpURLConnection connection)
    {
        this.connection = connection;

    }

    public HttpURLConnection getConnection()
    {
        return connection;
    }

    public void close()
    {
        if (connection != null)
        {
            try
            {
                connection.disconnect();
            } catch (Exception e)
            {
            }
        }
    }

}
