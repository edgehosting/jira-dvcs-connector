package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;

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
    private static final short HTTP_STATUS_CODE_UNAUTHORIZED = 401;
    private static final short HTTP_STATUS_CODE_NOT_FOUND    = 404;
    

    protected final String apiUrl;

    private static Logger log = LoggerFactory.getLogger(BaseRemoteRequestor.class);

    public BaseRemoteRequestor(String apiUrl)
    {
        super();
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
        return requestWithoutPayload(HttpMethod.DELETE, uri, new HashMap<String, String>());
    }

    @Override
    public RemoteResponse post(String uri, Map<String, String> parameters)
    {
        return requestWithPayload(HttpMethod.POST, uri, parameters, false);
    }

    @Override
    public RemoteResponse post(String uri, Object payload)
    {
        return requestWithPayload(HttpMethod.POST, uri, payload, true);
    }

    @Override
    public RemoteResponse put(String uri, Map<String, String> parameters)
    {
        return requestWithPayload(HttpMethod.PUT, uri, parameters, false);
    }

    @Override
    public RemoteResponse put(String uri, Object payload)
    {
        return requestWithPayload(HttpMethod.PUT, uri, payload, true);
    }

    // --------------------------------------------------------------------------------------------------
    // extension hooks
    // --------------------------------------------------------------------------------------------------
    /**
     * E.g. append basic auth headers ...
     */
    protected void onConnectionCreated(HttpURLConnection connection, HttpMethod method) throws IOException
    {

    }

    /**
     * E.g. append oauth params ...
     */
    protected String afterFinalUriConstructed(HttpMethod forMethod, String finalUri)
    {
        return finalUri;
    }

    // --------------------------------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------------------------------

	private RemoteResponse requestWithPayload(HttpMethod postOrPut, String uri, Object params, boolean isJson)
	{
		HttpURLConnection connection = null;
		try
		{
			connection = createConnection(postOrPut, uri);
			if (isJson) {
				setPayloadOrParams(connection, params, true);
			} else {
				setPayloadOrParams(connection, params, false);
			}
			return checkAndCreateRemoteResponse(connection);
			
		} catch (BitbucketRequestException e)
        {
            throw e; // Unauthorized or NotFound exceptions will be rethrown
        }catch (Exception e)
		{
			// TODO log + message
			throw new BitbucketRequestException("Failed to execute request " + connection, e);
		}
	}

	private RemoteResponse requestWithoutPayload(HttpMethod getOrDelete, String uri, Map<String, String> parameters)
	{
		HttpURLConnection connection = null;
		try
		{
			connection = createConnection(getOrDelete, uri + paramsToString(parameters, uri.contains("?")));
			return checkAndCreateRemoteResponse(connection);

		} catch (Exception e)
		{
			// TODO log + message
			throw new BitbucketRequestException("Failed to execute request " + connection, e);
		}
	}

    private RemoteResponse checkAndCreateRemoteResponse(HttpURLConnection connection) throws IOException
	{
		RemoteResponse response = new RemoteResponse();
		
        if (connection.getResponseCode() >= 400)
        {
            switch (connection.getResponseCode())
            {
                case HTTP_STATUS_CODE_UNAUTHORIZED:
                    throw new BitbucketRequestException.Unauthorized();

                case HTTP_STATUS_CODE_NOT_FOUND:
                    throw new BitbucketRequestException.NotFound();

                default:
                    throw new BitbucketRequestException("Error response code during the request : " + connection.getResponseCode());
            }
        }
		
		response.setHttpStatusCode(connection.getResponseCode());
		response.setResponse(connection.getInputStream());
		return response;
	}

    protected void logRequest(HttpURLConnection connection)
    {
        log.debug("[{} :: {}]", new Object[] { connection.getRequestMethod(), connection.getURL() });
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
            for (Iterator<Map.Entry<String, String>> iterator = parameters.entrySet().iterator(); iterator.hasNext();)
            {
                Map.Entry<String, String> entry = iterator.next();
                queryStringBuilder.append(encode(entry.getKey()));
                queryStringBuilder.append("=");
                queryStringBuilder.append(encode(entry.getValue()));
                if (iterator.hasNext())
                {
                    queryStringBuilder.append("&");
                }
            }
        }
        return queryStringBuilder.toString();
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
        } catch (Exception e)
        {
            throw new BitbucketRequestException("Required encoding not found", e);
        }
    }

    private HttpURLConnection createConnection(HttpMethod method, String uri) throws IOException
    {
        HttpURLConnection connection = method.createConnection(afterFinalUriConstructed(method, apiUrl + uri));
        //
        logRequest(connection);
        //
        //
        // something to extend
        //
        onConnectionCreated(connection, method);

        return connection;
    }

    @SuppressWarnings("all")
    private void setPayloadOrParams(HttpURLConnection connection, Object params, boolean isJson) throws IOException
    {
        connection.setDoOutput(true);

        if (params != null)
        {
            byte[] data = new byte[] {};

            if (isJson)
            {
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                data = ClientUtils.toJson(params).getBytes("UTF-8");
            } else
            {
                // assuming post/put kind of "form" params
                data = paramsToString((Map) params, true).getBytes("UTF-8");
            }

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
