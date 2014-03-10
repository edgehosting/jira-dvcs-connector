package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

import org.apache.http.client.HttpClient;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests over {@link HttpClientProvider} implementation.
 *
 * @author Miroslav Stencel
 *
 */
public class HttpClientProviderTest
{
    private HttpClientProvider httpClientProvider;

    /**
     * Prepares test environment.
     *
     * @throws Exception
     */
    @BeforeMethod
    public void before() throws Exception
    {
         httpClientProvider = new HttpClientProvider();
    }

    @Test
    public void testSharedInstance()
    {
        HttpClient httpClient1 = httpClientProvider.getHttpClient();
        HttpClient httpClient2 = httpClientProvider.getHttpClient();
        HttpClient httpClient3 = httpClientProvider.getHttpClient(false);

        Assert.assertEquals(httpClient1, httpClient2, "Two HttpClient instances should be identical.");
        Assert.assertEquals(httpClient1, httpClient3, "Two HttpClient instances should be identical.");

        HttpClient cachedHttpClient1 = httpClientProvider.getHttpClient(true);
        HttpClient cachedHttpClient2 = httpClientProvider.getHttpClient(true);

        Assert.assertEquals(cachedHttpClient1, cachedHttpClient2, "Two cached HttpClient instances should be identical.");
    }
}
