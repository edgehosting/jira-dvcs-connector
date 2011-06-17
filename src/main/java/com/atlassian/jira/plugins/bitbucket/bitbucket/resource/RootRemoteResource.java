package com.atlassian.jira.plugins.bitbucket.bitbucket.resource;

import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketException;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketResourceNotFoundException;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.security.auth.trustedapps.Null;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

/**
 */
public class RootRemoteResource implements RemoteResource
{
    private final Logger logger = LoggerFactory.getLogger(RootRemoteResource.class);
    private final String baseUrl;
    private final HttpClient httpClient;

    public RootRemoteResource(String baseUrl)
    {
        this.baseUrl = baseUrl;
        this.httpClient = new HttpClient();
    }

    /**
     * Loads a remote resource and returns the body parsed as a JSON object a successful load
     *
     * @param uri the url to load, the {@link #baseUrl} is prepended
     * @return the body of the response
     */
    public JSONObject get(String uri)
    {
        return get(uri, null);
    }

    /**
     * Loads a remote resource and returns the body parsed as a JSON object a successful load
     *
     * @param uri the url to load, the {@link #baseUrl} is prepended
     * @return the body of the response
     */
    public JSONObject get(String uri, Map<String, Object> params)
    {
        GetMethod get = null;

        try
        {
            StringBuffer queryString = new StringBuffer();

            if (params != null && !params.isEmpty())
            {
                for (Map.Entry<String, Object> entry : params.entrySet())
                {
                    if(queryString.length()>0)
                        queryString.append("&");
                    else
                        queryString.append("?");
                    queryString.append(entry.getKey());
                    queryString.append("=");
                    queryString.append(URLEncoder.encode(String.valueOf(entry.getValue()), "UTF-8"));
                }
            }

            String url = baseUrl + uri + queryString.toString();
            get = new GetMethod(url);
            logger.debug("get remote resource [ " + url + " ]");

            int status = httpClient.executeMethod(get);
            if (HttpStatus.SC_OK == status)
                return new JSONObject(get.getResponseBodyAsString());
            else if (HttpStatus.SC_NOT_FOUND == status)
                throw new BitbucketResourceNotFoundException("resource [ " + url + " ] not found");
            else
                throw new BitbucketException("unhandled http response code [ " + status + " ] for [ " + url + " ]");

        }
        catch (HttpException e)
        {
            throw new BitbucketException("could not contact bitbucket", e);
        }
        catch (IOException e)
        {
            throw new BitbucketException("could not contact bitbucket", e);
        }
        catch (JSONException e)
        {
            throw new BitbucketException("could not parse bitbucket response", e);
        } finally {
            if(get!=null)
                get.releaseConnection();
        }
    }
}
