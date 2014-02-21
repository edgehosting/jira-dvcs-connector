package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.util;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;

/**
 * Client used for httpclient version &lt; 4.2 
 */
public class ThreadSafeHttpClient extends DefaultHttpClient
{
    
    @Override
    protected ClientConnectionManager createClientConnectionManager()
    {
        return new ThreadSafeClientConnManager(createHttpParams(), createDefaultSchemeRegistry());
    }

    private SchemeRegistry createDefaultSchemeRegistry() {
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(
                new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        registry.register(
                new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        return registry;
    }
}
