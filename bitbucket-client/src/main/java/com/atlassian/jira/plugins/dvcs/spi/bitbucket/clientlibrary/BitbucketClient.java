package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.util.SystemUtils;
import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

public class BitbucketClient
{
    private final Logger log = LoggerFactory.getLogger(BitbucketClient.class);

    private static final String UTF8 = "UTF-8";

    private static final Type NO_RETURN_TYPE = new Type() {};

    private static enum HttpMethod { GET, POST, DELETE };

    private static enum HttpStatusCode {
        OK        ((short) 200),
        NO_CONTENT((short) 204);

        final short statusCode;

        private HttpStatusCode(short statusCode)
        {
            this.statusCode = statusCode;
        }
    }

    private static final Gson GSON = new GsonBuilder().create();

    private final String apiUrl;
    private String username;
    private String password;

    public BitbucketClient(String apiUrl)
    {
        this.apiUrl = apiUrl;
    }

    public Object get(String resourceUrl, Type type) throws BitbucketClientException
    {
        try
        {
            return executeHttpRequest(resourceUrl, HttpMethod.GET, HttpStatusCode.OK, type);
        } catch (IOException e)
        {
            throw new BitbucketClientException("Error retrieving data from [" + apiUrl + resourceUrl + "]", e);
        }
    }

    public void delete(String resourceUrl) throws BitbucketClientException
    {
        try
        {
            executeHttpRequest(resourceUrl, HttpMethod.DELETE, HttpStatusCode.NO_CONTENT, NO_RETURN_TYPE);
        } catch (IOException e)
        {
            throw new BitbucketClientException("Error calling DELETE on [" + apiUrl + resourceUrl + "]", e);
        }
    }

    public Object post(String resourceUrl, List<String> params, Type type) throws BitbucketClientException
    {
        try
        {
            return executeHttpRequest(resourceUrl, HttpMethod.POST, HttpStatusCode.OK, params, type);
        } catch (IOException e)
        {
            throw new BitbucketClientException("Error posting data to [" + apiUrl + resourceUrl + "]", e);
        }
    }

    private <T> T executeHttpRequest(String resourceUrl, HttpMethod requestMethod, HttpStatusCode expectedResponseCode,
            Type returnType) throws IOException
    {
        return executeHttpRequest(resourceUrl,
                                  requestMethod,
                                  expectedResponseCode,
                                  Collections.<String>emptyList(), // no request parameters
                                  returnType);
    }

    private <T> T executeHttpRequest(String resourceUrl, HttpMethod requestMethod, HttpStatusCode expectedResponseCode,
            List<String> requestParameters, Type returnType) throws IOException
    {
        URL finalUrl = new URL(apiUrl + resourceUrl);

        HttpURLConnection urlConnection = (HttpURLConnection) finalUrl.openConnection();
        urlConnection.setRequestMethod(requestMethod.name());
        addAuthorisation(urlConnection);

        addParameters(urlConnection, requestParameters);

        checkResponseCode(urlConnection, expectedResponseCode);

        switch (requestMethod) {
            case GET:
            case POST:
                return parseJson(urlConnection.getInputStream(), returnType);

            case DELETE:
                return null; // delete method does not return anything

            default:
                throw new IllegalStateException("Request method " + requestMethod + " is not supported !");
        }
    }

    private void checkResponseCode(HttpURLConnection connection, HttpStatusCode expectedResponseCode) throws IOException {
        final int responseCode = connection.getResponseCode();
        log.debug("Calling [" + connection.getRequestMethod() + " " + connection.getURL() + " ] returned code ["
                + responseCode + "]");

        if (responseCode != expectedResponseCode.statusCode)
        {
            throw new IOException("Unexpected response code: " + responseCode);
        }
    }

    private void addParameters(HttpURLConnection request, List<String> params) throws IOException
    {
        if (params.isEmpty())
        {
            return;
        }
        request.setDoOutput(true);
        String allParams = Joiner.on("&").join(params);
        byte[] data = allParams.getBytes(UTF8);
        request.setFixedLengthStreamingMode(data.length);
        BufferedOutputStream output = new BufferedOutputStream(request.getOutputStream());
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
                // Ignored
            }
        }
    }

    /**
     * @param username
     * @param password
     */
    public void setAuthorisation(String username, String password)
    {
        this.username = username;
        this.password = password;
    }


    /**
     * @param connection
     * @throws UnsupportedEncodingException
     */
    private void addAuthorisation(HttpURLConnection connection) throws UnsupportedEncodingException
    {
        connection.setRequestProperty("Authorization", "Basic "
            + SystemUtils.encodeUsingBase64(username + ":" + password).replaceAll("\n", "").replaceAll("\r", ""));
    }

    /**
     * @param stream
     * @param type
     * @return
     * @throws IOException
     */
    private <T> T parseJson(InputStream stream, Type type) throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, UTF8));
        try
        {
            return GSON.fromJson(reader, type);
        } catch (JsonParseException e)
        {
            throw new IOException("Parse exception converting JSON to object", e);
        } finally
        {
            try
            {
                reader.close();
            } catch (IOException e)
            {
                // nothing
            }
        }
    }
}
