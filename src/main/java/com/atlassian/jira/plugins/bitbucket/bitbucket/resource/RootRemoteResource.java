package com.atlassian.jira.plugins.bitbucket.bitbucket.resource;

import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketException;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;
import com.atlassian.sal.api.net.ResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 */
public class RootRemoteResource implements RemoteResource
{
    private final Logger logger = LoggerFactory.getLogger(RootRemoteResource.class);
    private final RequestFactory<?> requestFactory;
    private final String username;
    private final String password;
    private final String baseUrl;

    public RootRemoteResource(RequestFactory<?> requestFactory, String baseUrl)
    {
        this(requestFactory, null, null, baseUrl);
    }

    public RootRemoteResource(RequestFactory<?> requestFactory, String username, String password, String baseUrl)
    {
        this.requestFactory = requestFactory;
        this.username = username;
        this.password = password;
        this.baseUrl = baseUrl;
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
        try
        {
            Request<?> request = requestFactory.createRequest(Request.MethodType.GET, baseUrl + uri);

            if (params != null && !params.isEmpty())
            {
                String[] queryParams = new String[params.size() * 2];
                int index = 0;
                for (Map.Entry<String, Object> entry : params.entrySet())
                {
                    queryParams[index] = entry.getKey();
                    queryParams[index + 1] = String.valueOf(entry.getValue());
                    index += 2;
                }
                request.addRequestParameters(queryParams);
            }

            if (username != null && password != null)
                request.addBasicAuthentication(username, password);

            return new JSONObject(request.execute());
        }
        catch (JSONException e)
        {
            throw new BitbucketException("could not parse bitbucket response", e);
        }
        catch (ResponseException e)
        {
            throw new BitbucketException("could not parse bitbucket response", e);
        }
    }
}
