package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteResponse implements Serializable
{

    private static final long serialVersionUID = -8160018795770610703L;
    private final Logger log = LoggerFactory.getLogger(RemoteResponse.class);

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
                closeInputStream(connection.getInputStream());
            } catch (IOException e)
            {
                log.warn("Error closing input stream " + e, e);
            }
            try
            {
                closeInputStream(connection.getErrorStream());
            } catch (IOException e)
            {
                log.warn("Error closing error stream " + e, e);
            }
            connection.disconnect();
        }

    }

    private void closeInputStream(InputStream is) throws IOException
    {
        if (is == null)
        {
            return;
        }
        try
        {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int ret = 0;
            byte[] buf = new byte[4096];
            // read the response body
            while ((ret = is.read(buf)) > 0)
            {
                os.write(buf, 0, ret);
            }
        } finally
        {
            // close the errorstream
            is.close();
        }
    }

}
