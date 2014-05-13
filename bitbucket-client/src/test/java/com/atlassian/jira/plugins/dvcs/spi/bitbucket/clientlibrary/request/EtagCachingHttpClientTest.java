package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.cache.HeaderConstants;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HttpContext;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Date;

public class EtagCachingHttpClientTest
{
    private static final String ETAG = "testetag";
    private static final String TEST_RESPONSE = "Test response";
    
    @Mock
    private HttpClient httpClient;    
    
    @BeforeMethod
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    class EtagAnswer implements Answer<HttpResponse>
    {
        private boolean modified;
        private Date lastModified;
        
        @Override
        public HttpResponse answer(InvocationOnMock invocation) throws Throwable
        {
            Object[] args =invocation.getArguments();
            HttpUriRequest request = (HttpUriRequest) args[0];
            
            Header etagHeader = request.getFirstHeader(HeaderConstants.IF_NONE_MATCH);
            Header modifiedSinceHeader = request.getFirstHeader(HeaderConstants.IF_MODIFIED_SINCE);
            HttpResponse response;
            if ((etagHeader != null && ETAG.equals(etagHeader.getValue())) || (modifiedSinceHeader != null && lastModified != null && DateUtils.parseDate(modifiedSinceHeader.getValue()).before(lastModified)))
            {
                response = new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_MODIFIED, "Not Modified");
                response.setHeader(HeaderConstants.ETAG, ETAG);
                if (lastModified != null)
                {
                    response.setHeader(HeaderConstants.LAST_MODIFIED, DateUtils.formatDate(lastModified));
                }
                modified = false;
            } else
            {
                response = new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "Ok");
                response.setEntity(new StringEntity(TEST_RESPONSE));
                response.setHeader(HeaderConstants.ETAG, ETAG);
                lastModified = new Date();
                response.setHeader(HeaderConstants.LAST_MODIFIED, DateUtils.formatDate(lastModified));
                modified = true;
            }
             
            return response;
        }
        
        public boolean isModified()
        {
            return modified;
        }
        
        public Date getLastModified()
        {
            return lastModified;
        }
    }
    
    @Test
    public void execute() throws ClientProtocolException, IOException
    {
        EtagAnswer answer = new EtagAnswer();
        
        Mockito.when(httpClient.execute(Matchers.any(HttpUriRequest.class), Matchers.any(HttpContext.class))).then(answer);
        
        EtagCachingHttpClient client = new EtagCachingHttpClient(httpClient);
        
        HttpResponse response = client.execute(prepareRequest(null, null));
        checkResponse(response, DateUtils.formatDate(answer.getLastModified()));
        Assert.assertTrue(answer.isModified());
        
        response = client.execute(prepareRequest(null, null));
        checkResponse(response, DateUtils.formatDate(answer.getLastModified()));
        Assert.assertFalse(answer.isModified());
    }
    
    @Test
    public void executeWithEtag() throws ClientProtocolException, IOException
    {
        EtagAnswer answer = new EtagAnswer();
        
        Mockito.when(httpClient.execute(Matchers.any(HttpUriRequest.class), Matchers.any(HttpContext.class))).then(answer);
        
        EtagCachingHttpClient client = new EtagCachingHttpClient(httpClient);
        
        HttpResponse response = client.execute(prepareRequest(ETAG, null));
        Header etag = response.getFirstHeader(HeaderConstants.ETAG);
        Assert.assertNotNull(etag, "Etag should not be null");
        Assert.assertEquals(etag.getValue(), ETAG);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_NOT_MODIFIED, "Http status must be 304 Not Modified");
        Assert.assertNull(response.getEntity());
        Assert.assertFalse(answer.isModified());
    }
    
    @Test
    public void executeWithModifiedSince() throws ClientProtocolException, IOException
    {
        EtagAnswer answer = new EtagAnswer();
        
        Mockito.when(httpClient.execute(Matchers.any(HttpUriRequest.class), Matchers.any(HttpContext.class))).then(answer);
        
        EtagCachingHttpClient client = new EtagCachingHttpClient(httpClient);
        
        HttpResponse response = client.execute(prepareRequest(null, null));
        checkResponse(response, DateUtils.formatDate(answer.getLastModified()));
        Assert.assertTrue(answer.isModified());
        Header lastModifiedHeader = response.getFirstHeader(HeaderConstants.LAST_MODIFIED);
        String lastModified = lastModifiedHeader.getValue();
        
        response = client.execute(prepareRequest(null, lastModified));
        checkResponse(response, lastModified);
        Assert.assertFalse(answer.isModified());
    }
    
    private void checkResponse(HttpResponse response, String lastModified) throws IllegalStateException, IOException
    {
        Header etag = response.getFirstHeader(HeaderConstants.ETAG);
        Header lastModifiedHeader = response.getFirstHeader(HeaderConstants.LAST_MODIFIED);
        Assert.assertNotNull(etag, "Etag should not be null");
        Assert.assertEquals(etag.getValue(), ETAG);
        Assert.assertNotNull(lastModifiedHeader, "Last modified header should not be null");
        Assert.assertEquals(lastModifiedHeader.getValue(), lastModified);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK, "Http status must be 200 Ok");
        Assert.assertEquals(getStringFromEntity(response.getEntity()), TEST_RESPONSE);
    }
    
    private String getStringFromEntity(HttpEntity entity) throws IllegalStateException, IOException
    {
        return IOUtils.toString(entity.getContent(), "UTF-8");
    }
    
    private HttpUriRequest prepareRequest(String etag, String lastModified)
    {
        HttpUriRequest request = new HttpGet("https://test.com/test?test=test");
        if (etag != null)
        {
            request.addHeader(HeaderConstants.IF_NONE_MATCH, etag);
        }
        
        if (lastModified != null)
        {
            request.addHeader(HeaderConstants.IF_MODIFIED_SINCE, lastModified);
        }
        return request;
    }
}
