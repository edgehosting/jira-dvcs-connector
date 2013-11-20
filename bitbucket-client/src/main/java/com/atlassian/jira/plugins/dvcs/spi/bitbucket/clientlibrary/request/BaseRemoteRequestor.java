package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BadRequestRetryer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.cache.HttpCacheStorage;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.cache.BasicHttpCacheStorage;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpProtocolParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

/**
 * BaseRemoteRequestor
 * 
 * 
 * <br />
 * <br />
 * Created on 13.7.2012, 10:25:24 <br />
 * <br />
 * 
 * @author jhocman@atlassian.com
 * 
 */
public class BaseRemoteRequestor implements RemoteRequestor
{
    private final Logger log = LoggerFactory.getLogger(BaseRemoteRequestor.class);

    private static final int DEFAULT_CONNECT_TIMEOUT = Integer.getInteger("bitbucket.client.connection.timeout", 30000);
    private static final int DEFAULT_SOCKET_TIMEOUT = Integer.getInteger("bitbucket.client.socket.timeout", 60000);

    private int connectionTimeout = DEFAULT_CONNECT_TIMEOUT;
    private int socketTimeout = DEFAULT_SOCKET_TIMEOUT;

    protected final ApiProvider apiProvider;
    private final HttpClientProxyConfig proxyConfig;

    private static HttpCacheStorage storage;

    private final boolean cached;

    public BaseRemoteRequestor(ApiProvider apiProvider)
    {
        this.apiProvider = apiProvider;
        this.proxyConfig = new HttpClientProxyConfig();
        this.cached = apiProvider.isCached();
        if (apiProvider.getTimeout() >= 0)
        {
            this.connectionTimeout = apiProvider.getTimeout();
            this.socketTimeout = apiProvider.getTimeout();
        }
    }
    
    @Override
    public <T> T get(String uri, Map<String, String> parameters, ResponseCallback<T> callback)
    {
        return getWithRetry(uri, parameters, callback);
    }

    @Override
    public <T> T delete(String uri, Map<String, String> parameters, ResponseCallback<T> callback)
    {
        return deleteWithRetry(uri, parameters, callback);
    }

    @Override
    public  <T> T post(String uri, Map<String, ? extends Object> parameters, ResponseCallback<T> callback)
    {
        return postWithRetry(uri, parameters, callback);
    }

    @Override
    public <T> T post(final String uri, final String body, final ContentType contentType, final ResponseCallback<T> callback)
    {
        HttpPost method = new HttpPost();
        return requestWithBody(method, uri, body, contentType, callback);
    }


    @Override
    public <T> T put(String uri, Map<String, String> parameters, ResponseCallback<T> callback)
    {
        return putWithRetry(uri, parameters, callback);
    }

    // --------------------------------------------------------------------------------------------------
    // Retryers...
    // --------------------------------------------------------------------------------------------------
    
    private <T> T getWithRetry(final String uri, final Map<String, String> parameters,
            final ResponseCallback<T> callback)
    {
        return new BadRequestRetryer<T>().retry(new Callable<T>()
        {
            @Override
            public T call() throws Exception
            {
                HttpGet getMethod = new HttpGet();
                return requestWithoutPayload(getMethod, uri, parameters, callback);
            }
        });
    }

    private <T> T deleteWithRetry(final String uri, final Map<String, String> parameters,
            final ResponseCallback<T> callback)
    {
        return new BadRequestRetryer<T>().retry(new Callable<T>()
        {
            @Override
            public T call() throws Exception
            {
                HttpDelete method = new HttpDelete();
                return requestWithoutPayload(method, uri, parameters, callback);
            }
        });
    }

    private <T> T postWithRetry(final String uri, final Map<String, ? extends Object> parameters,
                                final ResponseCallback<T> callback)
    {
        return new BadRequestRetryer<T>().retry(new Callable<T>()
        {
            @Override
            public T call() throws Exception
            {
                HttpPost method = new HttpPost();
                return requestWithPayload(method, uri, parameters, callback);
            }
        });
    }

    private <T> T putWithRetry(final String uri, final Map<String, String> parameters,
            final ResponseCallback<T> callback)
    {
        return new BadRequestRetryer<T>().retry(new Callable<T>()
        {
            @Override
            public T call() throws Exception
            {
                HttpPut method = new HttpPut();
                return requestWithPayload(method, uri, parameters, callback);
            }
        });
    }

    // --------------------------------------------------------------------------------------------------
    // extension hooks
    // --------------------------------------------------------------------------------------------------
    /**
     * E.g. append basic auth headers ...
     */
    protected void onConnectionCreated(HttpClient client, HttpRequestBase method, Map<String, ? extends Object> params)
            throws IOException
    {

    }

    /**
     * E.g. append oauth params ...
     */
    protected String afterFinalUriConstructed(HttpRequestBase method, String finalUri,  Map<String, ? extends Object> params)
    {
        return finalUri;
    }

    // --------------------------------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------------------------------

    protected void logRequest(HttpRequestBase method, String finalUrl, Map<String, ? extends Object> params)
    {
        final StringBuilder sb = new StringBuilder("{");
        processParams(params, new ParameterProcessor()
        {
            @Override
            public void process(String key, String value)
            {
                if (sb.length() > 1)
                {
                    sb.append(",");
                }
                sb.append(key).append("=").append(value);
            }
        });

        sb.append("}");

        if (log.isDebugEnabled())
        {
            log.debug("[REST call {} {}, Params: {} \nHeaders: {}]", new Object[] { method.getMethod(), finalUrl, sb.toString(), sanitizeHeadersForLogging(method.getAllHeaders()) });
        }
    }

    private <T> T requestWithPayload(HttpEntityEnclosingRequestBase method, String uri, Map<String, ? extends Object> params, ResponseCallback<T> callback)
    {
        HttpClient client = newDefaultHttpClient();
        RemoteResponse response = null;
       
        try
        {
            createConnection(client, method, uri, params);
            setPayloadParams(method, params);

            HttpResponse httpResponse = client.execute(method);
            response = checkAndCreateRemoteResponse(method, client, httpResponse);

            return callback.onResponse(response);

        } catch (BitbucketRequestException e)
        {
            throw e; // Unauthorized or NotFound exceptions will be rethrown
        } catch (IOException e)
        {
            log.debug("Failed to execute request: " + method.getURI(), e);
            throw new BitbucketRequestException("Failed to execute request " + method.getURI(), e);
        } catch (URISyntaxException e)
        {
            log.debug("Failed to execute request: " + method.getURI(), e);
            throw new BitbucketRequestException("Failed to execute request " + method.getURI(), e);
        } finally
        {
            closeResponse(response);
        }
    }

    private <T> T requestWithBody(HttpEntityEnclosingRequestBase method, String uri, String body, ContentType contentType, ResponseCallback<T> callback)
    {
        HttpClient client = newDefaultHttpClient();
        RemoteResponse response = null;

        try
        {
            createConnection(client, method, uri, null);
            setBody(method, body, contentType);

            HttpResponse httpResponse = client.execute(method);
            response = checkAndCreateRemoteResponse(method, client, httpResponse);

            return callback.onResponse(response);

        } catch (BitbucketRequestException e)
        {
            throw e; // Unauthorized or NotFound exceptions will be rethrown
        } catch (IOException e)
        {
            log.debug("Failed to execute request: " + method.getURI(), e);
            throw new BitbucketRequestException("Failed to execute request " + method.getURI(), e);
        } catch (URISyntaxException e)
        {
            log.debug("Failed to execute request: " + method.getURI(), e);
            throw new BitbucketRequestException("Failed to execute request " + method.getURI(), e);
        } finally
        {
            closeResponse(response);
        }
    }

    private void closeResponse(RemoteResponse response)
    {
        if (response != null)
        {
            response.close();
        }
    }

    private <T> T requestWithoutPayload(HttpRequestBase method, String uri, Map<String, String> parameters, ResponseCallback<T> callback)
    {
        HttpClient client = newDefaultHttpClient();
        if (cached)
        {
            client = new EtagCachingHttpClient(client, getStorage());
        }
        RemoteResponse response = null;
       
        try
        {
            createConnection(client, method, uri + paramsToString(parameters, uri.contains("?")), parameters);
          
            HttpResponse httpResponse = client.execute(method);
            response = checkAndCreateRemoteResponse(method, client, httpResponse);
            
            return callback.onResponse(response);

        } catch (IOException e)
        {
            log.debug("Failed to execute request: " + method.getURI(), e);
            throw new BitbucketRequestException("Failed to execute request " + method.getURI(), e);
            
        } catch (URISyntaxException e)
        {
            log.debug("Failed to execute request: " + method.getURI(), e);
            throw new BitbucketRequestException("Failed to execute request " + method.getURI(), e);
        } finally
        {
            closeResponse(response);
        }
    }

    private RemoteResponse checkAndCreateRemoteResponse(HttpRequestBase method, HttpClient client, HttpResponse httpResponse) throws IOException
    {
        
        RemoteResponse response = new RemoteResponse();

        int statusCode = httpResponse.getStatusLine().getStatusCode();
        if (statusCode >= 300)
        {
            logRequestAndResponse(method, httpResponse, statusCode);
            
            RuntimeException toBeThrown = new BitbucketRequestException.Other("Error response code during the request : "
                    + statusCode);            
             
            switch (statusCode)
            {
            case HttpStatus.SC_BAD_REQUEST:
                toBeThrown = new BitbucketRequestException.BadRequest_400();
                break;
            case HttpStatus.SC_UNAUTHORIZED:
                toBeThrown = new BitbucketRequestException.Unauthorized_401();
                break;
            case HttpStatus.SC_FORBIDDEN:
                toBeThrown = new BitbucketRequestException.Forbidden_403();
                break;
            case HttpStatus.SC_NOT_FOUND:
                toBeThrown = new BitbucketRequestException.NotFound_404(method.getMethod() + " " + method.getURI());
                break;
            }
            

            throw toBeThrown;
        }

        response.setHttpStatusCode(statusCode);
        if (httpResponse.getEntity() != null)
        {
            response.setResponse(httpResponse.getEntity().getContent());
        }
        response.setHttpClient(client);
        return response;
    }

    private void logRequestAndResponse(HttpRequestBase method, HttpResponse httpResponse, int statusCode) throws IOException
    {
        String responseAsString = null;
        if (httpResponse.getEntity() != null)
        {
            InputStream is = httpResponse.getEntity().getContent();
            StringWriter writer = new StringWriter();
            IOUtils.copy(is, writer, "UTF-8");
            responseAsString = writer.toString();
        }

        if (log.isWarnEnabled())
        {
            log.warn("Failed to properly execute request [{} {}], \nParams: {}, \nResponse code {}",
                    new Object[] { method.getMethod(), method.getURI(), method.getParams(), statusCode });
        }

        if (log.isDebugEnabled())
        {
            log.debug("Failed to properly execute request [{} {}], \nHeaders: {}, \nParams: {}, \nResponse code {}, response: {}",
                    new Object[] { method.getMethod(), method.getURI(), sanitizeHeadersForLogging(method.getAllHeaders()), method.getParams(),
                            statusCode, responseAsString });
        }
    }

    private Header[] sanitizeHeadersForLogging(Header[] headers)
    {
        List<Header> result = new LinkedList<Header>(Arrays.asList(headers));
        Iterator<Header> iterator = result.iterator();
        while (iterator.hasNext())
        {
            if (iterator.next().getName().toLowerCase().contains("authorization"))
            {
                iterator.remove();
            }
        }
        return result.toArray(new Header[result.size()]);
    }

    protected String paramsToString(Map<String, String> parameters, boolean urlAlreadyHasParams)
    {
        StringBuilder queryStringBuilder = new StringBuilder();

        if (parameters != null && !parameters.isEmpty())
        {
            if (!urlAlreadyHasParams)
            {
                queryStringBuilder.append("?");
            } else
            {
                queryStringBuilder.append("&");
            }

            paramsMapToString(parameters, queryStringBuilder);
        }
        return queryStringBuilder.toString();
    }

    private void paramsMapToString(Map<String, String> parameters, StringBuilder builder)
    {
        for (Iterator<Map.Entry<String, String>> iterator = parameters.entrySet().iterator(); iterator.hasNext();)
        {
            Map.Entry<String, String> entry = iterator.next();
            builder.append(encode(entry.getKey()));
            builder.append("=");
            builder.append(encode(entry.getValue()));
            if (iterator.hasNext())
            {
                builder.append("&");
            }
        }
    }

    private static String encode(String str)
    {
        if (str == null)
        {
            return null;
        }

        try
        {
            return URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e)
        {
            throw new BitbucketRequestException("Required encoding not found", e);
        }
    }

    private void createConnection(HttpClient client, HttpRequestBase method, String uri, Map<String, ? extends Object> params)
            throws IOException, URISyntaxException
    {
        if (StringUtils.isNotBlank(apiProvider.getUserAgent()))
        {
            HttpProtocolParams.setUserAgent(client.getParams(), apiProvider.getUserAgent());
        }

        HttpConnectionParams.setConnectionTimeout(client.getParams(), connectionTimeout);
        HttpConnectionParams.setSoTimeout(client.getParams(), socketTimeout);

        String remoteUrl;
        if (uri.startsWith("http:/") || uri.startsWith("https:/")) {
            remoteUrl = uri;

        } else {
            String apiUrl = uri.startsWith("/api/") ? apiProvider.getHostUrl() : apiProvider.getApiUrl();
            remoteUrl = apiUrl + uri;
        }

        proxyConfig.configureProxy(client, remoteUrl);
        String finalUrl = afterFinalUriConstructed(method, remoteUrl, params);
        method.setURI(new URI(finalUrl));
        //
        logRequest(method, finalUrl, params);
        //
        // something to extend
        //
        onConnectionCreated(client, method, params);

    }

    protected HttpClient newDefaultHttpClient()
    {
        return new DefaultHttpClient();
    }

    protected interface ParameterProcessor
    {
        void process(String key, String value);
    }

    private void setPayloadParams(HttpEntityEnclosingRequestBase method, Map<String, ? extends Object> params) throws IOException
    {
        if (params != null)
        {
            final List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            processParams(params, new ParameterProcessor()
            {
                @Override
                public void process(String key, String value)
                {
                    formparams.add(new BasicNameValuePair(key, value));
                }

            });

            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
            method.setEntity(entity);
        }
    }

    private void setBody(HttpEntityEnclosingRequestBase method, String body, ContentType contentType)
    {
        if (body != null)
        {
            StringEntity entity = new StringEntity(body, contentType);
            method.setEntity(entity);
        }
    }

    protected void processParams(Map<String, ? extends Object> params, ParameterProcessor processParameter)
    {
        if (params == null)
        {
            return;
        }

        for (Entry<String, ? extends Object> entry : params.entrySet())
        {
            Object value = entry.getValue();
            if (value instanceof Collection)
            {
                for (Object v : (Collection<?>) value)
                {
                    if (v != null)
                    {
                        processParameter.process(entry.getKey(), v.toString());
                    }
                }
            } else
            {
                if (value != null)
                {
                    processParameter.process(entry.getKey(), value.toString());
                }
            }
        }
    }

    private static synchronized HttpCacheStorage getStorage()
    {
        if (storage == null)
        {
            CacheConfig config = new CacheConfig();
            // if max cache entries value is not present the CacheConfig's default (CacheConfig.DEFAULT_MAX_CACHE_ENTRIES = 1000) will be used
            Integer maxCacheEntries = Integer.getInteger("bitbucket.client.cache.maxentries");
            if (maxCacheEntries != null)
            {
                config.setMaxCacheEntries(maxCacheEntries);
            }
            storage = new BasicHttpCacheStorage(config);
        }

        return storage;
    }
}