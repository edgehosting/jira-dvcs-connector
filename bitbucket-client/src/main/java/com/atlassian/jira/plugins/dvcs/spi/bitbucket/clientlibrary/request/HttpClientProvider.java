package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.conn.ProxySelectorRoutePlanner;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpProtocolParams;

import java.net.ProxySelector;
import javax.annotation.PreDestroy;

/**
 * Provides shared HttpClient instance
 *
 */
public class HttpClientProvider
{
    private static final int DEFAULT_CONNECT_TIMEOUT = Integer.getInteger("bitbucket.client.connection.timeout", 30000);
    private static final int DEFAULT_SOCKET_TIMEOUT = Integer.getInteger("bitbucket.client.socket.timeout", 60000);
    private static final int DEFAULT_MAX_TOTAL = Integer.getInteger("bitbucket.client.conmanager.maxtotal", 200);
    private static final int DEFAULT_MAX_PER_ROUTE = Integer.getInteger("bitbucket.client.conmanager.maxperroute", 100);

    private final AbstractHttpClient httpClient;

    public HttpClientProvider()
    {
        PoolingClientConnectionManager connectionManager = new PoolingClientConnectionManager();
        connectionManager.setMaxTotal(DEFAULT_MAX_TOTAL);
        connectionManager.setDefaultMaxPerRoute(DEFAULT_MAX_PER_ROUTE);

        httpClient = new DefaultHttpClient(connectionManager);

        HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), DEFAULT_CONNECT_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpClient.getParams(), DEFAULT_SOCKET_TIMEOUT);


        ProxySelectorRoutePlanner routePlanner = new ProxySelectorRoutePlanner(httpClient.getConnectionManager().getSchemeRegistry(), ProxySelector.getDefault());
        httpClient.setRoutePlanner(routePlanner);
    }

    public void setUserAgent(String userAgent)
    {
        HttpProtocolParams.setUserAgent(httpClient.getParams(), userAgent);
    }

    public HttpClient getHttpClient()
    {
        return httpClient;
    }

    @PreDestroy
    private void destroy()
    {
        httpClient.getConnectionManager().shutdown();
    }
}
