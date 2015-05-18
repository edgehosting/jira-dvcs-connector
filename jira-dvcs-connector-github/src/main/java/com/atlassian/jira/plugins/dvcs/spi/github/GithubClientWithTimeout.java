package com.atlassian.jira.plugins.dvcs.spi.github;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.GitHubRequest;
import org.eclipse.egit.github.core.client.GitHubResponse;

import java.io.IOException;
import java.net.HttpURLConnection;

import static java.lang.Long.parseLong;

/**
 * Github Client with the connection and read timeout set up
 */
public class GithubClientWithTimeout extends GitHubClient
{
    private static final int DEFAULT_CONNECT_TIMEOUT = Integer.getInteger("dvcs.connector.github.connection.timeout", 30000);
    private static final int DEFAULT_SOCKET_TIMEOUT = Integer.getInteger("dvcs.connector.github.socket.timeout", 60000);
    private static final String RATE_LIMIT_URI = "/rate_limit";

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

    public RateLimit getRateLimit()
    {
        GitHubRequest request = new GitHubRequest().setUri(RATE_LIMIT_URI);
        try
        {
            GitHubResponse response = get(request);
            return new RateLimit(getRequestLimit(), getRemainingRequests(), parseLong(response.getHeader("X-RateLimit-Reset")));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
