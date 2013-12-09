package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.Serializable;

public class RemoteResponse implements Serializable
{

    private static final long serialVersionUID = -8160018795770610703L;
    private final Logger log = LoggerFactory.getLogger(RemoteResponse.class);

    private InputStream inputStream;

    private int httpStatusCode;

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
    }

}
