package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.mockito.Matchers.anyBoolean;

/**
 * Unit tests over {@link BaseRemoteRequestor} implementation.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class BaseRemoteRequestorTest
{

    /**
     * Tested object.
     */
    private RemoteRequestor testedObject;

    /**
     * Mocked HTTP client - necessary to catch request execution.
     */
    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpClientProvider httpClientProvider;

    @Mock
    private ClientConnectionManager connectionManager;

    /**
     * Captures performed requests.
     */
    @Captor
    private ArgumentCaptor<HttpUriRequest> httpUriRequest;

    /**
     * Prepares test environment.
     * 
     * @throws Exception
     */
    @BeforeMethod
    public void before() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        ApiProvider apiProvider = Mockito.mock(ApiProvider.class);
        testedObject = new BaseRemoteRequestor(apiProvider, httpClientProvider);

        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);

        Mockito.doReturn(connectionManager).when(httpClient).getConnectionManager();
        Mockito.doReturn(httpResponse).when(httpClient).execute(Mockito.<HttpUriRequest> any());
        Mockito.doReturn(new BasicHttpParams()).when(httpClient).getParams();
        Mockito.doReturn(statusLine).when(httpResponse).getStatusLine();

        Mockito.doReturn("http://bitbucket.org").when(apiProvider).getHostUrl();
        Mockito.doReturn("http://bitbucket.org/api").when(apiProvider).getApiUrl();
        Mockito.doReturn(httpClient).when(httpClientProvider).getHttpClient();
        Mockito.doReturn(httpClient).when(httpClientProvider).getHttpClient(anyBoolean());
    }

    /**
     * Tests that relative URI is build up properly.
     * 
     * @throws Exception
     */
    @Test
    public void testRelativeUriAPI() throws Exception
    {
        @SuppressWarnings("unchecked")
        ResponseCallback<Void> callback = Mockito.mock(ResponseCallback.class);
        testedObject.get("/test", Collections.<String, String> emptyMap(), callback);
        Mockito.verify(httpClient).execute(httpUriRequest.capture());

        Assert.assertEquals("http://bitbucket.org/api/test", httpUriRequest.getValue().getURI().toString());
    }

    /**
     * Tests that relative URI is build up properly.
     * 
     * @throws Exception
     */
    @Test
    public void testRelativeUri() throws Exception
    {
        @SuppressWarnings("unchecked")
        ResponseCallback<Void> callback = Mockito.mock(ResponseCallback.class);
        testedObject.get("/api/test", Collections.<String, String> emptyMap(), callback);
        Mockito.verify(httpClient).execute(httpUriRequest.capture());

        Assert.assertEquals("http://bitbucket.org/api/test", httpUriRequest.getValue().getURI().toString());
    }

    /**
     * Tests that absolute URI is build up properly.
     * 
     * @throws Exception
     */
    @Test
    public void testAbsoluteUri() throws Exception
    {
        @SuppressWarnings("unchecked")
        ResponseCallback<Void> callback = Mockito.mock(ResponseCallback.class);
        testedObject.get("http://internal.bitbucket.org/api/test", Collections.<String, String> emptyMap(), callback);
        Mockito.verify(httpClient).execute(httpUriRequest.capture());

        Assert.assertEquals("http://internal.bitbucket.org/api/test", httpUriRequest.getValue().getURI().toString());
    }

}
