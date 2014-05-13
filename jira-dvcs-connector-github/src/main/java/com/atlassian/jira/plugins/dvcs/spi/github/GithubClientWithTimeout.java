package com.atlassian.jira.plugins.dvcs.spi.github;

import org.eclipse.egit.github.core.client.GitHubClient;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Github Client with the connection and read timeout set up
 *
 * @author Miroslav Stencel <mstencel@atlassian.com>
 *
 */
public class GithubClientWithTimeout extends GitHubClient
{
    private static final int DEFAULT_CONNECT_TIMEOUT = Integer.getInteger("dvcs.connector.github.connection.timeout", 30000);
    private static final int DEFAULT_SOCKET_TIMEOUT = Integer.getInteger("dvcs.connector.github.socket.timeout", 60000);

    private int connectionTimeout = DEFAULT_CONNECT_TIMEOUT;
    private int socketTimeOut = DEFAULT_SOCKET_TIMEOUT;

    public GithubClientWithTimeout(String host, int i, String protocol)
    {
        super(host, i, protocol);
    }

    @Override
    protected HttpURLConnection createConnection(String uri) throws IOException
    {
        HttpURLConnection connection = super.createConnection(uri);
        connection.setConnectTimeout(connectionTimeout);
        connection.setReadTimeout(socketTimeOut);
        return connection;
    }

    public void setTimeout(int timeout)
    {
        this.connectionTimeout = timeout;
        this.socketTimeOut = timeout;
    }

    public void setConnectionTimeout(int connectionTimeout)
    {
        this.connectionTimeout = connectionTimeout;
    }

    public void setSocketTimeOut(int socketTimeOut)
    {
        this.socketTimeOut = socketTimeOut;
    }

}
