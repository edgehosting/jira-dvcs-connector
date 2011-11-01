package com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
        return get(auth, "/repositories/" + encode(owner) + "/" + encode(slug), null, uri.getApiUrl());
    }

    public String getChangeset(SourceControlRepository repository, String id)
    {
    	RepositoryUri uri = RepositoryUri.parse(repository.getUrl());
    	String owner = uri.getOwner();
    	String slug = uri.getSlug();
    	Authentication auth = authenticationFactory.getAuthentication(repository);

    	logger.debug("parse changeset [ {} ] [ {} ] [ {} ]", new String[]{owner, slug, id});
        return get(auth, "/repositories/" + encode(owner) + "/" + encode(slug) + "/changesets/" + encode(id), null, uri.getApiUrl());
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
		return get(auth, "/repositories/" + encode(owner) + "/" + encode(slug) + "/changesets", params, uri.getApiUrl());
    }

    public String getUser(SourceControlRepository repository, String username)
    {
    	RepositoryUri uri = RepositoryUri.parse(repository.getUrl());
        logger.debug("parse user [ {} ]", username);
        return get(Authentication.ANONYMOUS, "/users/" + encode(username), null, uri.getApiUrl());
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
    
    private String buildQueryString(Map<String, Object> params)
    {
    	try
		{
			StringBuilder queryStringBuilder = new StringBuilder();
			
			if (params != null && !params.isEmpty())
			{
				queryStringBuilder.append("?");
				for (Iterator<Map.Entry<String, Object>> iterator = params.entrySet().iterator(); iterator.hasNext(); )
				{
					Map.Entry<String, Object> entry = iterator.next();
					queryStringBuilder.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
					queryStringBuilder.append("=");
					queryStringBuilder.append(URLEncoder.encode(String.valueOf(entry.getValue()), "UTF-8"));
					if (iterator.hasNext())
						queryStringBuilder.append("&");
				}
			}
			return queryStringBuilder.toString();
		} catch (UnsupportedEncodingException e)
		{
			throw new SourceControlException("required encoding not found");
		}
    }

    private String get(Authentication auth, String urlPath, Map<String, Object> params, String apiBaseUrl)
    {
    	return runRequest(Request.MethodType.GET, apiBaseUrl, urlPath, auth, params, null);
    }

    private String post(Authentication auth, String urlPath, String postData, String apiBaseUrl)
    {
    	return runRequest(Request.MethodType.POST, apiBaseUrl, urlPath, auth, null, postData);
    }

    private void delete(Authentication auth, String apiUrl, String urlPath)
	{
		runRequest(Request.MethodType.DELETE, apiUrl, urlPath, auth, null, null);
	}

    
    private String runRequest(Request.MethodType methodType, String apiBaseUrl, String urlPath, Authentication auth, Map<String, Object> params, String postData)
    {
    	String url = apiBaseUrl + urlPath + buildQueryString(params);
    	logger.debug("get [ " + url + " ]");
        try
        {
            Request<?, ?> request = requestFactory.createRequest(methodType, url);
            if (auth != null)
                auth.addAuthentication(request);
            if (postData!=null)
            	request.setRequestBody(postData);
            request.setSoTimeout(60000);
            return request.execute();
        }
        catch (ResponseException e)
        {
            throw new SourceControlException("could not parse bitbucket response [ " + url + " ]", e);
        }
    }


	public void setupPostcommitHook(SourceControlRepository repo, String postCommitUrl)
	{
		
		RepositoryUri uri = RepositoryUri.parse(repo.getUrl());
		Authentication auth = Authentication.basic(repo.getAdminUsername(), repo.getAdminPassword());
		String urlPath =  "/repositories/"+uri.getOwner()+"/"+uri.getSlug()+"/services";
		String apiUrl = uri.getApiUrl();
		String postData = "type=post;URL=" + postCommitUrl;

		post(auth, urlPath, postData, apiUrl);
	}

	public void removePostcommitHook(SourceControlRepository repo, String postCommitUrl)
	{
		RepositoryUri uri = RepositoryUri.parse(repo.getUrl());
		Authentication auth = Authentication.basic(repo.getAdminUsername(), repo.getAdminPassword());
		String urlPath =  "/repositories/"+uri.getOwner()+"/"+uri.getSlug()+"/services";
		String apiUrl = uri.getApiUrl();
		// Find the hook 
		try
		{
			String responseString = get(auth, urlPath, null, apiUrl);
			JSONArray jsonArray = new JSONArray(responseString);
			for (int i = 0; i < jsonArray.length(); i++)
			{
				JSONObject data = (JSONObject) jsonArray.get(i);
				String id = data.getString("id");
				JSONObject service = data.getJSONObject("service");
				JSONArray fields = service.getJSONArray("fields");
				JSONObject fieldData = (JSONObject) fields.get(0);
				String name = fieldData.getString("name");
				String value = fieldData.getString("value");
				if ("URL".equals(name) && postCommitUrl.equals(value))
				{
					// We have the hook, lets remove it
					delete(auth, apiUrl, urlPath+"/"+id);
				}
			}
		} catch (JSONException e)
		{
			logger.warn("Error removing postcommit service [{}]", e.getMessage());
		} catch (SourceControlException e)
		{
			logger.warn("Error removing postcommit service [{}]", e.getMessage());
		}
	}

}
