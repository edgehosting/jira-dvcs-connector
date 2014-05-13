package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.cache.HeaderConstants;
import org.apache.http.client.cache.HttpCacheEntry;
import org.apache.http.client.cache.HttpCacheStorage;
import org.apache.http.client.cache.Resource;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.cache.BasicHttpCacheStorage;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.HeapResourceFactory;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Date;

public class EtagCachingHttpClient implements HttpClient
{
    private final Logger log = LoggerFactory.getLogger(EtagCachingHttpClient.class);

    private final HttpClient backingHttpClient;
    private final HttpCacheStorage storage;

    public EtagCachingHttpClient()
    {
        this(new DefaultHttpClient(), new BasicHttpCacheStorage(new CacheConfig()));
    }

    public EtagCachingHttpClient(HttpCacheStorage storage)
    {
        this(new DefaultHttpClient(), storage);
    }

    public EtagCachingHttpClient(HttpClient httpClient)
    {
        this(httpClient, new BasicHttpCacheStorage(new CacheConfig()));
    }

    public EtagCachingHttpClient(HttpClient httpClient, HttpCacheStorage storage)
    {
        this.backingHttpClient = httpClient;
        this.storage = storage;
    }

    @Override
    public HttpParams getParams()
    {
        return backingHttpClient.getParams();
    }

    @Override
    public ClientConnectionManager getConnectionManager()
    {
        return backingHttpClient.getConnectionManager();
    }

    @Override
    public HttpResponse execute(HttpUriRequest request) throws IOException, ClientProtocolException
    {
        return execute(request, (HttpContext)null);
    }

    @Override
    public HttpResponse execute(HttpUriRequest request, HttpContext context) throws IOException, ClientProtocolException
    {
        if (!HeaderConstants.GET_METHOD.equals(request.getRequestLine().getMethod()))
        {
            // this is not GET method - can't be cached
            return backingHttpClient.execute(request);
        }

        Header requestEtagHeader = request.getFirstHeader(HeaderConstants.IF_NONE_MATCH);
        Header requestLastModifiedHeader = request.getFirstHeader(HeaderConstants.LAST_MODIFIED);
        HttpCacheEntry httpCacheEntry = null;
        
        // If request doesn't contain Etag nor Last modified date, we take them from cache 
        if (requestEtagHeader == null && requestLastModifiedHeader == null)
        {
            httpCacheEntry = storage.getEntry(generateKey(request));
    
            if (httpCacheEntry != null)
            {
                Header etagHeader = httpCacheEntry.getFirstHeader(HeaderConstants.ETAG);
                if (etagHeader != null)
                {
                    request.setHeader(HeaderConstants.IF_NONE_MATCH, etagHeader.getValue());
                }
    
                Header lastModifiedHeader = httpCacheEntry.getFirstHeader(HeaderConstants.LAST_MODIFIED);
                if (lastModifiedHeader != null)
                {
                    request.setHeader(HeaderConstants.IF_MODIFIED_SINCE, lastModifiedHeader.getValue());
                }
            }
        }

        Date requestDate = new Date();
        HttpResponse httpResponse = backingHttpClient.execute(request, context);

        if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_MODIFIED)
        {
            if (httpCacheEntry != null)
            {
                log.debug("Generating response from cache.");
                return generateResponse(httpCacheEntry);
            } else
            {
                // etag or last modified date were specified in request, we just return not modified response
                return httpResponse;
            }
        }

        return cacheResponse(request, httpResponse, requestDate,  new Date());
    }

    @Override
    public HttpResponse execute(HttpHost target, HttpRequest request) throws IOException, ClientProtocolException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpResponse execute(HttpHost target, HttpRequest request, HttpContext context) throws IOException, ClientProtocolException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException
    {
        return null;
    }

    @Override
    public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException, ClientProtocolException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException,
            ClientProtocolException
    {
        throw new UnsupportedOperationException();
    }

    private boolean isResponseCacheable(HttpResponse response)
    {
        return HttpStatus.SC_OK == response.getStatusLine().getStatusCode();
    }

    private String generateKey(HttpRequest request)
    {
        return generateKeyFromUri(request.getRequestLine().getUri());
    }

    public String generateKeyFromUri(String uri)
    {
        try
        {
            URL u = new URL(uri);

            String protocol = u.getProtocol().toLowerCase();
            String hostname = u.getHost().toLowerCase();
            int port = getCanonicalPort(u.getPort(), protocol);
            String path = getCanonicalPath(u.getPath());

            if ("".equals(path))
            {
                path = "/";
            }

            String query = u.getQuery();
            String file = (query != null) ? (path + "?" + query) : path;
            return new URL(protocol, hostname, port, file).toString();
        } catch (MalformedURLException e)
        {
        }

        return uri;
    }

    private String getCanonicalPath(String path)
    {
        try
        {
            String decodedPath = URLDecoder.decode(path, "UTF-8");
            return (new URI(decodedPath)).getPath();
        } catch (URISyntaxException e)
        {
        } catch (UnsupportedEncodingException e)
        {
        }
        return path;
    }

    private int getCanonicalPort(int port, String protocol)
    {
        if (port == -1 && "http".equalsIgnoreCase(protocol))
        {
            return 80;
        } else if (port == -1 && "https".equalsIgnoreCase(protocol))
        {
            return 443;
        }
        return port;
    }

    private HttpResponse generateResponse(HttpCacheEntry entry) throws IOException
    {

        HttpResponse response = new BasicHttpResponse(entry.getProtocolVersion(), entry
                .getStatusCode(), entry.getReasonPhrase());

        Resource resource = entry.getResource();
        InputStreamEntity entity = new InputStreamEntity(resource.getInputStream(), resource.length());
        entity.setContentType(entry.getFirstHeader(HTTP.CONTENT_TYPE));
        entity.setContentEncoding(entry.getFirstHeader(HTTP.CONTENT_ENCODING));
        response.setHeaders(entry.getAllHeaders());
        response.setEntity(entity);

        return response;
    }

    public HttpResponse cacheResponse(HttpRequest request,
            HttpResponse response, Date requestSent, Date responseReceived)
            throws IOException
    {
        if (!isResponseCacheable(response))
        {
            return response;
        }

        HttpCacheEntry entry = new HttpCacheEntry(
                requestSent,
                responseReceived,
                response.getStatusLine(),
                response.getAllHeaders(),
                createResource(request, response));
        storeInCache(request, entry);
        return generateResponse(entry);
    }

    private Resource createResource(HttpRequest request, HttpResponse response) throws IOException
    {
        HttpEntity entity = response.getEntity();
        if (entity == null)
        {
            return null;
        }
        String uri = request.getRequestLine().getUri();
        InputStream instream = entity.getContent();
        return new HeapResourceFactory().generate(uri, instream, null);
    }
    
    private void storeInCache(HttpRequest request, HttpCacheEntry entry) throws IOException
    {
            String uri = generateKey(request);
            storage.putEntry(uri, entry);
    }
}
