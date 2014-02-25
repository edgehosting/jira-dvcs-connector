package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.util;

import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.pool.ConnPoolControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Martin Skurla mskurla@atlassian.com
 */
public class SystemUtils
{
    private static final boolean IS_HTTP_CLIENT_GE_4_2;
    
    private static final Logger log = LoggerFactory.getLogger(SystemUtils.class);
    
    private static final int DEFAULT_MAX_TOTAL = Integer.getInteger("bitbucket.client.conmanager.maxtotal", 20);
    private static final int DEFAULT_MAX_PER_ROUTE = Integer.getInteger("bitbucket.client.conmanager.maxperroute", 15);
    
    static
    {
        IS_HTTP_CLIENT_GE_4_2 = getIsHttpClientGe42();
    }
    
    private SystemUtils() {}
    
    private static boolean getIsHttpClientGe42()
    {
        try {
            Class.forName("org.apache.http.impl.conn.PoolingClientConnectionManager");
            return true;
        } catch (ClassNotFoundException e)
        {
        }
        return false;
    }

    public static final String encodeUsingBase64(String input)
    {
        try
        {
            byte[] encodedBytes = Base64.encodeBase64(input.getBytes("UTF-8"));

            return new String(encodedBytes, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new AssertionError();
        }
    }
    
    public static AbstractHttpClient createHttpClient()
    {
        try
        {
            if (IS_HTTP_CLIENT_GE_4_2)
            {
                ClientConnectionManager poolingManager = (ClientConnectionManager) Class.forName(
                        "org.apache.http.impl.conn.PoolingClientConnectionManager").newInstance();
                ((ConnPoolControl<?>) poolingManager).setMaxTotal(DEFAULT_MAX_TOTAL);
                ((ConnPoolControl<?>) poolingManager).setDefaultMaxPerRoute(DEFAULT_MAX_PER_ROUTE);
                return new DefaultHttpClient(poolingManager, (HttpParams) null);
            } else {
                return new ThreadSafeHttpClient();
            }
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Can not create http client.", e);
        }
    }
    
    public static void releaseConnection(HttpRequestBase request, HttpResponse response)
    {
        if (IS_HTTP_CLIENT_GE_4_2)
        {
            request.releaseConnection();
        }
    }
}
