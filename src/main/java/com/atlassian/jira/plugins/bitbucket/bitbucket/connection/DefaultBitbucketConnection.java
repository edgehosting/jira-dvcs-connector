package com.atlassian.jira.plugins.bitbucket.bitbucket.connection;

import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketAuthentication;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketException;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;
import com.atlassian.sal.api.net.ResponseException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * The default implementation uses an injected {@link com.atlassian.sal.api.net.RequestFactory}.
 */
public class DefaultBitbucketConnection implements BitbucketConnection
{
    private static final String BASE_URL = "https://api.bitbucket.org/1.0/";
    private final RequestFactory<?> requestFactory;

    public DefaultBitbucketConnection(RequestFactory<?> requestFactory)
    {
        this.requestFactory = requestFactory;
    }

    public String getRepository(BitbucketAuthentication auth, String owner, String slug)
    {
        return get(auth, "repositories/" + encode(owner) + "/" + encode(slug), null);
    }

    public String getChangeset(BitbucketAuthentication auth, String owner, String slug, String id)
    {
        return get(auth, "repositories/" + encode(owner) + "/" + encode(slug) + "/changesets/" + encode(id), null);
    }

    public String getChangesets(BitbucketAuthentication auth, String owner, String slug, String start, int limit)
    {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("limit", String.valueOf(limit));
        params.put("start", encode(start));
        return get(auth, "repositories/" + encode(owner) + "/" + encode(slug) + "/changesets", params);
    }

    public String getUser(String username)
    {
        return get(BitbucketAuthentication.ANONYMOUS, "users/" + encode(username), null);
    }

    private String encode(String s)
    {
        try
        {
            return URLEncoder.encode(s, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new BitbucketException("required encoding not found", e);
        }
    }

    private String get(BitbucketAuthentication auth, String uri, Map<String, Object> params)
    {
        try
        {
            Request<?,?> request = requestFactory.createRequest(Request.MethodType.GET, BASE_URL + uri);

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

            if (auth != null)
                auth.addAuthentication(request);

            return request.execute();
        }
        catch (ResponseException e)
        {
            throw new BitbucketException("could not parse bitbucket response [ "+uri+" ]", e);
        }
    }

}
