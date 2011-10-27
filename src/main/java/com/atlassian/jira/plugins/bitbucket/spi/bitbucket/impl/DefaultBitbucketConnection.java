package com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.bitbucket.api.Authentication;
import com.atlassian.jira.plugins.bitbucket.api.AuthenticationFactory;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketConnection;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.RepositoryUri;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;
import com.atlassian.sal.api.net.ResponseException;

/**
 * The default implementation uses an injected {@link com.atlassian.sal.api.net.RequestFactory}.
 */
public class DefaultBitbucketConnection implements BitbucketConnection
{
    private final RequestFactory<?> requestFactory;
    private final Logger logger = LoggerFactory.getLogger(DefaultBitbucketConnection.class);
	private final AuthenticationFactory authenticationFactory;

    public DefaultBitbucketConnection(RequestFactory<?> requestFactory, AuthenticationFactory authenticationFactory)
    {
        this.requestFactory = requestFactory;
		this.authenticationFactory = authenticationFactory;
    }

    public String getRepository(SourceControlRepository repository)
    {
    	RepositoryUri uri = RepositoryUri.parse(repository.getUrl());
    	String owner = uri.getOwner();
    	String slug = uri.getSlug();
    	Authentication auth = authenticationFactory.getAuthentication(repository);

    	logger.debug("parse repository [ {} ] [ {} ]", uri.getOwner(), uri.getSlug());
        return get(auth, "repositories/" + encode(owner) + "/" + encode(slug), null, uri.getApiUrl());
    }

    public String getChangeset(SourceControlRepository repository, String id)
    {
    	RepositoryUri uri = RepositoryUri.parse(repository.getUrl());
    	String owner = uri.getOwner();
    	String slug = uri.getSlug();
    	Authentication auth = authenticationFactory.getAuthentication(repository);

    	logger.debug("parse changeset [ {} ] [ {} ] [ {} ]", new String[]{owner, slug, id});
        return get(auth, "repositories/" + encode(owner) + "/" + encode(slug) + "/changesets/" + encode(id), null, uri.getApiUrl());
    }

    public String getChangesets(SourceControlRepository repository, String startNode, int limit)
    {
    	RepositoryUri uri = RepositoryUri.parse(repository.getUrl());
        String owner = uri.getOwner();
		String slug = uri.getSlug();
		Authentication auth = authenticationFactory.getAuthentication(repository);
		
		logger.debug("parse changesets [ {} ] [ {} ] [ {} ] [ {} ]",
                new String[]{owner, slug, startNode, String.valueOf(limit)});
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("limit", String.valueOf(limit));
        if (startNode!=null)
        {
        	params.put("start", startNode);
        }
		return get(auth, "repositories/" + encode(owner) + "/" + encode(slug) + "/changesets", params, uri.getApiUrl());
    }

    public String getUser(SourceControlRepository repository, String username)
    {
    	RepositoryUri uri = RepositoryUri.parse(repository.getUrl());
        logger.debug("parse user [ {} ]", username);
        return get(Authentication.ANONYMOUS, "users/" + encode(username), null, uri.getApiUrl());
    }

    private String encode(String s)
    {
        try
        {
            return URLEncoder.encode(s, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new SourceControlException("required encoding not found", e);
        }
    }

    private String get(Authentication auth, String uri, Map<String, Object> params, String apiBaseUrl)
    {
        try
        {
            StringBuilder queryString = new StringBuilder();

            if (params != null && !params.isEmpty())
            {
                queryString.append("?");
                for (Iterator<Map.Entry<String, Object>> iterator = params.entrySet().iterator(); iterator.hasNext(); )
                {
                    Map.Entry<String, Object> entry = iterator.next();
                    queryString.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                    queryString.append("=");
                    queryString.append(URLEncoder.encode(String.valueOf(entry.getValue()), "UTF-8"));
                    if (iterator.hasNext())
                        queryString.append("&");
                }
            }

            String url = apiBaseUrl + "/" + uri + queryString.toString();
            logger.debug("get [ " + url + " ]");
            Request<?, ?> request = requestFactory.createRequest(Request.MethodType.GET, url);

            if (auth != null)
                auth.addAuthentication(request);

            request.setSoTimeout(60000);

            return request.execute();
        }
        catch (ResponseException e)
        {
            throw new SourceControlException("could not parse bitbucket response [ " + uri + " ]", e);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new SourceControlException("required encoding not found");
        }
    }

}
