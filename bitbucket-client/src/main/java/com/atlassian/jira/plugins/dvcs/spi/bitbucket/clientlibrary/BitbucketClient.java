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
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

public class BitbucketClient
{
    private final Logger log = LoggerFactory.getLogger(BitbucketClient.class);

    private static final String UTF8 = "UTF-8";
    private static final String HTTP_GET = "GET";
    private static final String HTTP_DELETE = "DELETE";
    private static final String HTTP_POST = "POST";
    
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
            URL url = new URL(apiUrl + resourceUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(HTTP_GET);
            addAuthorisation(connection);

            final int code = connection.getResponseCode();
			log.debug("Calling [" + connection.getRequestMethod() + " " + connection.getURL() + " ] returned code ["+code+"]");
            
            if (code != 200)
            {
            	throw new IOException("Unexpected response code: " + code);
            }

            return parseJson(connection.getInputStream(), type);
        } catch (IOException e)
        {
            throw new BitbucketClientException("Error retrieving data from [" + apiUrl + resourceUrl + "]", e);
        }
    }

    public void delete(String resourceUrl) throws BitbucketClientException
    {
        try
        {
            URL url = new URL(apiUrl + resourceUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(HTTP_DELETE);
            addAuthorisation(connection);

            final int code = connection.getResponseCode();
			log.debug("Calling [" + connection.getRequestMethod() + " " + connection.getURL() + " ] returned code ["+code+"]");
            if (code != 204)
            {
                throw new IOException("Unexpected response code: " + code);
            } 
        } catch (IOException e)
        {
            throw new BitbucketClientException("Error calling DELETE on [" + apiUrl + resourceUrl + "]", e);
        }
    }
    
    public RepositoryLink post(String resourceUrl, List<String> params, Type type) throws BitbucketClientException
    {
        try
        {
            URL url = new URL(apiUrl + resourceUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(HTTP_POST);
            addAuthorisation(connection);
            addParameters(connection, params);

            final int code = connection.getResponseCode();
			log.debug("Calling [" + connection.getRequestMethod() + " " + connection.getURL() + " ] returned code ["+code+"]");
            if (code != 200)
            {
            	throw new IOException("Unexpected response code: " + code);
            }

            return parseJson(connection.getInputStream(), type);
        } catch (IOException e)
        {
            throw new BitbucketClientException("Error posting data to [" + apiUrl + resourceUrl + "]", e);
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
            + Base64.encodeBase64String((username + ":" + password).getBytes(UTF8)).replaceAll("\n", "").replaceAll("\r", ""));
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
