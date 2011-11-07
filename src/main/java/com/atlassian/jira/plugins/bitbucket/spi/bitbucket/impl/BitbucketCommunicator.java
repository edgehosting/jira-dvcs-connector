package com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl;

import com.atlassian.jira.plugins.bitbucket.api.*;
import com.atlassian.jira.plugins.bitbucket.spi.Communicator;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketChangesetFactory;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;
import com.atlassian.sal.api.net.ResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/**
 * Starting point for remote API calls to the bitbucket remote API
 */
public class BitbucketCommunicator implements Communicator
{
    private final Logger logger = LoggerFactory.getLogger(BitbucketCommunicator.class);
    private final RequestFactory<?> requestFactory;
    private final AuthenticationFactory authenticationFactory;

    public BitbucketCommunicator(RequestFactory<?> requestFactory, AuthenticationFactory authenticationFactory)
    {
        this.requestFactory = requestFactory;
        this.authenticationFactory = authenticationFactory;
    }

    public SourceControlUser getUser(SourceControlRepository repository, String username)
    {
        try
        {
            RepositoryUri uri = repository.getRepositoryUri();
            logger.debug("parse user [ {} ]", username);

            String responseString = get(Authentication.ANONYMOUS, "/users/" + encode(username), null, uri.getApiUrl());
            return BitbucketUserFactory.parse(new JSONObject(responseString));
        } catch (SourceControlException e)
        {
            logger.debug("could not load user [ " + username + " ]");
            return SourceControlUser.UNKNOWN_USER;
        } catch (JSONException e)
        {
            logger.debug("could not load user [ " + username + " ]");
            return SourceControlUser.UNKNOWN_USER;
        }
    }

    public Changeset getChangeset(SourceControlRepository repository, String id)
    {
        try
        {
            RepositoryUri uri = repository.getRepositoryUri();
            String owner = uri.getOwner();
            String slug = uri.getSlug();
            Authentication auth = authenticationFactory.getAuthentication(repository);

            logger.debug("parse changeset [ {} ] [ {} ] [ {} ]", new String[] { owner, slug, id });
            String responseString = get(auth, "/repositories/" + encode(owner) + "/" + encode(slug) + "/changesets/" + encode(id), null,
                uri.getApiUrl());

            return BitbucketChangesetFactory.parse(repository.getId(), new JSONObject(responseString));
        } catch (JSONException e)
        {
            throw new SourceControlException("could not parse json result", e);
        }
    }

    public List<Changeset> getChangesets(final SourceControlRepository repository, String startNode, int limit)
    {
        RepositoryUri uri = repository.getRepositoryUri();
        String owner = uri.getOwner();
        String slug = uri.getSlug();
        Authentication auth = authenticationFactory.getAuthentication(repository);

        logger.debug("parse changesets [ {} ] [ {} ] [ {} ] [ {} ]", new String[] { owner, slug, startNode, String.valueOf(limit) });
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("limit", String.valueOf(limit));
        if (startNode != null)
        {
            params.put("start", startNode);
        }
        String responseString = get(auth, "/repositories/" + encode(owner) + "/" + encode(slug) + "/changesets", params, uri.getApiUrl());

        List<Changeset> changesets = new ArrayList<Changeset>();
        try
        {
            JSONArray list = new JSONObject(responseString).getJSONArray("changesets");
            for (int i = 0; i < list.length(); i++)
            {
                changesets.add(BitbucketChangesetFactory.parse(repository.getId(), list.getJSONObject(i)));
            }
        } catch (JSONException e)
        {
            throw new SourceControlException("Could not parse json object", e);
        }

        return changesets;
    }

    public void setupPostcommitHook(SourceControlRepository repo, String postCommitUrl)
    {
        RepositoryUri uri = repo.getRepositoryUri();
        Authentication auth = Authentication.basic(repo.getAdminUsername(), repo.getAdminPassword());
        String urlPath = "/repositories/" + uri.getOwner() + "/" + uri.getSlug() + "/services";
        String apiUrl = uri.getApiUrl();
        String postData = "type=post;URL=" + postCommitUrl;

        post(auth, urlPath, postData, apiUrl);
    }

    public void removePostcommitHook(SourceControlRepository repo, String postCommitUrl)
    {
        RepositoryUri uri = repo.getRepositoryUri();
        Authentication auth = Authentication.basic(repo.getAdminUsername(), repo.getAdminPassword());
        String urlPath = "/repositories/" + uri.getOwner() + "/" + uri.getSlug() + "/services";
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
                    delete(auth, apiUrl, urlPath + "/" + id);
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

    private String encode(String s)
    {
        try
        {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e)
        {
            throw new SourceControlException("required encoding not found", e);
        }
    }

    // --------------------------------------------------------------------------------------------------
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

    private String runRequest(Request.MethodType methodType, String apiBaseUrl, String urlPath, Authentication auth,
        Map<String, Object> params, String postData)
    {
        String url = apiBaseUrl + urlPath + buildQueryString(params);
        logger.debug("get [ " + url + " ]");
        try
        {
            Request<?, ?> request = requestFactory.createRequest(methodType, url);
            if (auth != null) auth.addAuthentication(request);
            if (postData != null) request.setRequestBody(postData);
            request.setSoTimeout(60000);
            return request.execute();
        } catch (ResponseException e)
        {
            throw new SourceControlException("could not parse bitbucket response [ " + url + " ]", e);
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
                for (Iterator<Map.Entry<String, Object>> iterator = params.entrySet().iterator(); iterator.hasNext();)
                {
                    Map.Entry<String, Object> entry = iterator.next();
                    queryStringBuilder.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                    queryStringBuilder.append("=");
                    queryStringBuilder.append(URLEncoder.encode(String.valueOf(entry.getValue()), "UTF-8"));
                    if (iterator.hasNext()) queryStringBuilder.append("&");
                }
            }
            return queryStringBuilder.toString();
        } catch (UnsupportedEncodingException e)
        {
            throw new SourceControlException("required encoding not found");
        }
    }

    public Iterable<Changeset> getChangesets(final SourceControlRepository repository)
    {
        return new Iterable<Changeset>()
        {
            public Iterator<Changeset> iterator()
            {
                return new BitbucketChangesetIterator(BitbucketCommunicator.this, repository);
            }
        };
    }

    public boolean isRepositoryValid(RepositoryUri repositoryUri)
    {
        String owner = repositoryUri.getOwner();
        String slug = repositoryUri.getSlug();

        logger.debug("parse repository [ {} ]", slug);
        String responseString = get(Authentication.ANONYMOUS, "/repositories/" + encode(owner) + "/" + encode(slug), null, repositoryUri.getApiUrl());

        try
        {
            String name = new JSONObject(responseString).getString("name");
            if (name.equalsIgnoreCase(repositoryUri.getSlug()))
            {
                return true;
            }
        } catch (JSONException e)
        {
            throw new SourceControlException("Could not parse json object", e);
        }

        return false;
    }

}
