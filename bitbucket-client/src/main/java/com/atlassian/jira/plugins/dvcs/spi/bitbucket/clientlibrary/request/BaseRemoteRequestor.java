package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BadRequestRetryer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
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
import java.util.Iterator;
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
    
    private static final int DEFAULT_CONNECT_TIMEOUT = Integer.getInteger("dvcs.connector.bitbucket.connection.timeout", 30000);
    private static final int DEFAULT_SOCKET_TIMEOUT = Integer.getInteger("dvcs.connector.bitbucket.socket.timeout", 60000);
    
    protected final ApiProvider apiProvider;
    private final HttpClientProxyConfig proxyConfig;

    public BaseRemoteRequestor(ApiProvider apiProvider)
    {
        this.apiProvider = apiProvider;
        this.proxyConfig = new HttpClientProxyConfig();
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
    public  <T> T post(String uri, Map<String, String> parameters, ResponseCallback<T> callback)
    {
        return postWithRetry(uri, parameters, callback);
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

    private <T> T postWithRetry(final String uri, final Map<String, String> parameters,
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
    protected void onConnectionCreated(DefaultHttpClient client, HttpRequestBase method, Map<String, String> params)
            throws IOException
    {

    }

    /**
     * E.g. append oauth params ...
     */
    protected String afterFinalUriConstructed(HttpRequestBase method, String finalUri, Map<String, String> params)
    {
        return finalUri;
    }

    // --------------------------------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------------------------------

    protected void logRequest(HttpRequestBase method, String finalUrl, Map<String, String> params)
    {
        log.debug("[REST call {} {}, Params: {} \nHeaders: {}]", new Object[] { method.getMethod(), finalUrl, params, method.getAllHeaders() });
    }

    private <T> T requestWithPayload(HttpEntityEnclosingRequestBase method, String uri, Map<String, String> params, ResponseCallback<T> callback)
    {
        DefaultHttpClient client = new DefaultHttpClient();
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

    private void closeResponse(RemoteResponse response)
    {
        if (response != null)
        {
            response.close();
        }
    }

    private <T> T requestWithoutPayload(HttpRequestBase method, String uri, Map<String, String> parameters, ResponseCallback<T> callback)
    {
        DefaultHttpClient client = new DefaultHttpClient();
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

    private RemoteResponse checkAndCreateRemoteResponse(HttpRequestBase method, DefaultHttpClient client, HttpResponse httpResponse) throws IOException
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
                toBeThrown = new BitbucketRequestException.NotFound_404();
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
        log.warn("Failed to properly execute request [{} {}], \nHeaders: {}, \nParams: {}, \nResponse code {}, response: {}", 
                new Object[] {method.getMethod(), method.getURI(), method.getAllHeaders(), method.getParams(), statusCode, responseAsString });
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

    private void createConnection(DefaultHttpClient client, HttpRequestBase method, String uri, Map<String, String> params)
            throws IOException, URISyntaxException
    {
        if (StringUtils.isNotBlank(apiProvider.getUserAgent()))
        {
            HttpProtocolParams.setUserAgent(client.getParams(), apiProvider.getUserAgent());
        }
        
        HttpConnectionParams.setConnectionTimeout(client.getParams(), DEFAULT_CONNECT_TIMEOUT);
        HttpConnectionParams.setSoTimeout(client.getParams(), DEFAULT_SOCKET_TIMEOUT);
        
        String apiUrl = uri.startsWith("/api/") ? apiProvider.getHostUrl() : apiProvider.getApiUrl();
        proxyConfig.configureProxy(client, apiUrl + uri);
        
        String finalUrl = afterFinalUriConstructed(method, apiUrl + uri, params);
        method.setURI(new URI(finalUrl)); 
        //
        logRequest(method, finalUrl, params);
        //
        // something to extend
        //
        onConnectionCreated(client, method, params);

    }

    private void setPayloadParams(HttpEntityEnclosingRequestBase method, Map<String, String> params) throws IOException
    {
        if (params != null)
        {
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            for (Entry<String, String> entry : params.entrySet())
            {
                formparams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
            method.setEntity(entity);
        }
    }
}
