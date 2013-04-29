package com.atlassian.jira.plugins.dvcs.spi.github;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.eclipse.egit.github.core.client.GitHubClient;

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
    
    public GithubClientWithTimeout(String host, int i, String protocol)
    {
        super(host, i, protocol);
    }

    @Override
    protected HttpURLConnection createConnection(String uri) throws IOException
    {
        HttpURLConnection connection = super.createConnection(uri);
        connection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
        connection.setReadTimeout(DEFAULT_SOCKET_TIMEOUT);
        return connection;
    }
}
