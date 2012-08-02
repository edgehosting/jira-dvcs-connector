package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final int HTTP_STATUS_CODE_UNAUTHORIZED = 401;
    private static final int HTTP_STATUS_CODE_FORBIDDEN = 403;
    private static final int HTTP_STATUS_CODE_NOT_FOUND = 404;

    private final Logger log = LoggerFactory.getLogger(BaseRemoteRequestor.class);

    protected final String apiUrl;

    public BaseRemoteRequestor(String apiUrl)
    {
        this.apiUrl = apiUrl;
    }

    @Override
    public RemoteResponse get(String uri, Map<String, String> parameters)
    {
        return requestWithoutPayload(HttpMethod.GET, uri, parameters);
    }

    @Override
    public RemoteResponse delete(String uri)
    {
        return requestWithoutPayload(HttpMethod.DELETE, uri, Collections.<String, String> emptyMap());
    }

    @Override
    public RemoteResponse post(String uri, Map<String, String> parameters)
    {
        return requestWithPayload(HttpMethod.POST, uri, parameters);
    }

    @Override
    public RemoteResponse put(String uri, Map<String, String> parameters)
    {
        return requestWithPayload(HttpMethod.PUT, uri, parameters);
    }

    // --------------------------------------------------------------------------------------------------
    // extension hooks
    // --------------------------------------------------------------------------------------------------
    /**
     * E.g. append basic auth headers ...
     */
    protected void onConnectionCreated(HttpURLConnection connection, HttpMethod method, Map<String, String> params)
            throws IOException
    {

    }

    /**
     * E.g. append oauth params ...
     */
    protected String afterFinalUriConstructed(HttpMethod forMethod, String finalUri, Map<String, String> params)
    {
        return finalUri;
    }

    // --------------------------------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------------------------------

    protected void logRequest(HttpURLConnection connection)
    {
        log.debug("[{} : {} :: {}]", new Object[] { getClass().getSimpleName(), connection.getRequestMethod(),
                connection.getURL() });
    }

    private RemoteResponse requestWithPayload(HttpMethod postOrPut, String uri, Map<String, String> params)
    {

        HttpURLConnection connection = null;
        try
        {
            connection = createConnection(postOrPut, uri, params);
            setPayloadParams(connection, params);

            return checkAndCreateRemoteResponse(connection);

        } catch (BitbucketRequestException e)
        {
            throw e; // Unauthorized or NotFound exceptions will be rethrown
        } catch (IOException e)
        {
            log.debug("Failed to execute request: " + connection, e);
            throw new BitbucketRequestException("Failed to execute request " + connection, e);
        }
    }

    private RemoteResponse requestWithoutPayload(HttpMethod getOrDelete, String uri, Map<String, String> parameters)
    {
        HttpURLConnection connection = null;
        try
        {
            connection = createConnection(getOrDelete, uri + paramsToString(parameters, uri.contains("?")), parameters);
            return checkAndCreateRemoteResponse(connection);

        } catch (IOException e)
        {
            log.debug("Failed to execute request: " + connection, e);
            throw new BitbucketRequestException("Failed to execute request " + connection, e);
        }
    }

    private RemoteResponse checkAndCreateRemoteResponse(HttpURLConnection connection) throws IOException
    {
        RemoteResponse response = new RemoteResponse();

        if (connection.getResponseCode() >= 300)
        {
            RuntimeException toBeThrown =  new BitbucketRequestException("Error response code during the request : "
                    + connection.getResponseCode());
            
            switch (connection.getResponseCode())
            {
            case HTTP_STATUS_CODE_UNAUTHORIZED:
                toBeThrown = new BitbucketRequestException.Unauthorized_401();

            case HTTP_STATUS_CODE_FORBIDDEN:
                toBeThrown = new BitbucketRequestException.Forbidden_403();

            case HTTP_STATUS_CODE_NOT_FOUND:
                toBeThrown = new BitbucketRequestException.NotFound_404();
            }
            
            // log.error("Failed to properly execute request [" + connection.getRequestMethod() + "] : " + connection, toBeThrown);
            throw toBeThrown;
        }

        response.setHttpStatusCode(connection.getResponseCode());
        response.setResponse(connection.getInputStream());
        return response;
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

    private HttpURLConnection createConnection(HttpMethod method, String uri, Map<String, String> params)
            throws IOException
    {
        String finalUrl = afterFinalUriConstructed(method, apiUrl + uri, params);

        HttpURLConnection connection = method.createConnection(finalUrl);

        //
        logRequest(connection);
        //
        //
        // something to extend
        //
        onConnectionCreated(connection, method, params);

        return connection;
    }

    @SuppressWarnings("all")
    private void setPayloadParams(HttpURLConnection connection, Map<String, String> params) throws IOException
    {
        connection.setDoOutput(true);

        if (params != null)
        {
            byte[] data = new byte[] {};

            // assuming post/put kind of "form" params
            StringBuilder paramsAsString = new StringBuilder();
            paramsMapToString(params, paramsAsString);

            data = paramsAsString.toString().getBytes("UTF-8");

            connection.setFixedLengthStreamingMode(data.length);
            BufferedOutputStream output = new BufferedOutputStream(connection.getOutputStream());
            try
            {

                output.write(data);
                output.flush();

            } finally
            {
                try
                {
                    output.close();
                } catch (IOException ignored)
                {
                    // nop
                }
            }
        } else
        {
            connection.setFixedLengthStreamingMode(0);
        }
    }
}