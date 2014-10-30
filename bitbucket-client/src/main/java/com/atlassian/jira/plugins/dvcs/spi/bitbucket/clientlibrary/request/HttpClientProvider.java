package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.util.SystemUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.cache.HttpCacheStorage;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.cache.BasicHttpCacheStorage;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.conn.ProxySelectorRoutePlanner;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.ProxySelector;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;

/**
 * Provides shared HttpClient instance
 *
 */
@Component
public class HttpClientProvider
{
    private final Logger log = LoggerFactory.getLogger(HttpClientProvider.class);

    private static final int DEFAULT_CONNECT_TIMEOUT = Integer.getInteger("bitbucket.client.connection.timeout", 30000);
    private static final int DEFAULT_SOCKET_TIMEOUT = Integer.getInteger("bitbucket.client.socket.timeout", 60000);
    private static final long DEFAULT_CONNECTION_KEEP_ALIVE_DURATION = Long.getLong("bitbucket.client.conmanager.keepalive", 120000);

    private final AbstractHttpClient httpClient;
    private EtagCachingHttpClient cachingHttpClient;

    private static class ConnectionKeepAliveStrategy extends DefaultConnectionKeepAliveStrategy
    {
        private final long defaultKeepAliveDuration;

        public ConnectionKeepAliveStrategy(long defaultKeepAliveDuration)
        {
            this.defaultKeepAliveDuration = defaultKeepAliveDuration;
        }

        public long getKeepAliveDuration(HttpResponse response, HttpContext context)
        {
            long keepAliveDuration = super.getKeepAliveDuration(response, context);

            if (keepAliveDuration == -1)
            {
                return defaultKeepAliveDuration;
            } else
            {
                return keepAliveDuration;
            }
        }
    }

    public HttpClientProvider()
    {
        httpClient = SystemUtils.createHttpClient();

        HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), DEFAULT_CONNECT_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpClient.getParams(), DEFAULT_SOCKET_TIMEOUT);

        ProxySelectorRoutePlanner routePlanner = new ProxySelectorRoutePlanner(httpClient.getConnectionManager().getSchemeRegistry(), ProxySelector.getDefault());
        httpClient.setRoutePlanner(routePlanner);
        httpClient.setKeepAliveStrategy(new ConnectionKeepAliveStrategy(DEFAULT_CONNECTION_KEEP_ALIVE_DURATION));
        cachingHttpClient = new EtagCachingHttpClient(httpClient, createStorage());
    }

    public void setUserAgent(String userAgent)
    {
        HttpProtocolParams.setUserAgent(httpClient.getParams(), userAgent);
    }

    public HttpClient getHttpClient()
    {
        return getHttpClient(false);
    }

    public HttpClient getHttpClient(boolean cached)
    {
        if (cached)
        {
            return cachingHttpClient;
        }
        return httpClient;
    }

    public void closeIdleConnections()
    {
        log.debug("Closing idle HttpClient connections");
        httpClient.getConnectionManager().closeIdleConnections(0, TimeUnit.MILLISECONDS);
    }

    private HttpCacheStorage createStorage()
    {
        CacheConfig config = new CacheConfig();
        // if max cache entries value is not present the CacheConfig's default (CacheConfig.DEFAULT_MAX_CACHE_ENTRIES = 1000) will be used
        Integer maxCacheEntries = Integer.getInteger("bitbucket.client.cache.maxentries");
        if (maxCacheEntries != null)
        {
            config.setMaxCacheEntries(maxCacheEntries);
        }
        return new BasicHttpCacheStorage(config);
    }

    @PreDestroy
    private void destroy()
    {
        log.debug("Shutting down HttpClient connection manager");
        httpClient.getConnectionManager().shutdown();
    }
}
