package com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.bitbucket.api.Authentication;
import com.atlassian.jira.plugins.bitbucket.api.AuthenticationFactory;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlUser;
import com.atlassian.jira.plugins.bitbucket.api.impl.BasicAuthentication;
import com.atlassian.jira.plugins.bitbucket.spi.Communicator;
import com.atlassian.jira.plugins.bitbucket.spi.CommunicatorHelper;
import com.atlassian.jira.plugins.bitbucket.spi.CommunicatorHelper.ExtendedResponse;
import com.atlassian.jira.plugins.bitbucket.spi.CommunicatorHelper.ExtendedResponseHandler;
import com.atlassian.jira.plugins.bitbucket.spi.CustomStringUtils;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.spi.UrlInfo;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketChangesetFactory;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketUserFactory;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.net.RequestFactory;
import com.atlassian.sal.api.net.ResponseException;

/**
 * Starting point for remote API calls to the bitbucket remote API
 */
public class BitbucketCommunicator implements Communicator
{
    private final Logger logger = LoggerFactory.getLogger(BitbucketCommunicator.class);

    private final AuthenticationFactory authenticationFactory;
    private final CommunicatorHelper communicatorHelper;

    public BitbucketCommunicator(RequestFactory<?> requestFactory, AuthenticationFactory authenticationFactory)
    {
        this.authenticationFactory = authenticationFactory;
        this.communicatorHelper = new CommunicatorHelper(requestFactory);
    }
    
    @Override
    public SourceControlUser getUser(SourceControlRepository repository, String username)
    {
        try
        {
            RepositoryUri uri = repository.getRepositoryUri();
            logger.debug("parse user [ {} ]", username);

            String responseString = communicatorHelper.get(Authentication.ANONYMOUS, "/users/" + CustomStringUtils.encode(username), null, uri.getApiUrl());
            return BitbucketUserFactory.parse(new JSONObject(responseString).getJSONObject("user"));
        } catch (ResponseException e)
        {
            logger.debug("could not load user [ " + username + " ]");
            return SourceControlUser.UNKNOWN_USER;
        } catch (JSONException e)
        {
            logger.debug("could not load user [ " + username + " ]");
            return SourceControlUser.UNKNOWN_USER;
        }
    }

    @Override
    public Changeset getChangeset(SourceControlRepository repository, String id)
    {
        try
        {
            RepositoryUri uri = repository.getRepositoryUri();
            String owner = uri.getOwner();
            String slug = uri.getSlug();
            Authentication auth = authenticationFactory.getAuthentication(repository);

            logger.debug("parse changeset [ {} ] [ {} ] [ {} ]", new String[] { owner, slug, id });
            String responseString = communicatorHelper.get(auth, "/repositories/" + CustomStringUtils.encode(owner) + "/" +
                    CustomStringUtils.encode(slug) + "/changesets/" + CustomStringUtils.encode(id), null,
                    uri.getApiUrl());

            return BitbucketChangesetFactory.parse(repository.getId(), new JSONObject(responseString));
        } catch (ResponseException e)
        {
            throw new SourceControlException("could not get result", e);
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

        logger.debug("parse bitbucket changesets [ {} ] [ {} ] [ {} ] [ {} ]", new String[] { owner, slug, startNode, String.valueOf(limit) });
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("limit", String.valueOf(limit));
        if (startNode != null)
        {
            params.put("start", startNode);
        }

        List<Changeset> changesets = new ArrayList<Changeset>();

        ExtendedResponseHandler responseHandler = new ExtendedResponseHandler();
        try
        {
            communicatorHelper.get(auth, "/repositories/" + CustomStringUtils.encode(owner) + "/" +
                    CustomStringUtils.encode(slug) + "/changesets", params, uri.getApiUrl(), responseHandler);
            ExtendedResponse extendedResponse = responseHandler.getExtendedResponse();

            if (extendedResponse.getStatusCode() == HttpStatus.SC_UNAUTHORIZED)
            {
                throw new SourceControlException("Incorrect credentials");
            } else if (extendedResponse.getStatusCode() == HttpStatus.SC_NOT_FOUND)
            {
                // no more changesets
                return Collections.emptyList();
            }

            JSONArray list = new JSONObject(extendedResponse.getResponseString()).getJSONArray("changesets");
            for (int i = 0; i < list.length(); i++)
            {
                changesets.add(BitbucketChangesetFactory.parse(repository.getId(), list.getJSONObject(i)));
            }            
        } catch (ResponseException e)
        {
            logger.debug("Could not get changesets from node: {}", startNode);
            throw new SourceControlException("Error requesting changesets. Node: " + startNode + ". [" + e.getMessage() + "]", e);
        } catch (JSONException e)
        {
            throw new SourceControlException("Could not parse json object", e);
        }
        return changesets;
    }

    @Override
    public void setupPostcommitHook(SourceControlRepository repo, String postCommitUrl)
    {
        RepositoryUri uri = repo.getRepositoryUri();
        Authentication auth = new BasicAuthentication(repo.getAdminUsername(), repo.getAdminPassword());
        String urlPath = "/repositories/" + uri.getOwner() + "/" + uri.getSlug() + "/services";
        String apiUrl = uri.getApiUrl();
        String postData = "type=post;URL=" + postCommitUrl;

        try
        {
            communicatorHelper.post(auth, urlPath, postData, apiUrl);
        } catch (ResponseException e)
        {
            throw new SourceControlException("Could not add postcommit hook",e);
        }
    }

    @Override
    public void removePostcommitHook(SourceControlRepository repo, String postCommitUrl)
    {
        RepositoryUri uri = repo.getRepositoryUri();
        Authentication auth = new BasicAuthentication(repo.getAdminUsername(), repo.getAdminPassword());
        String urlPath = "/repositories/" + uri.getOwner() + "/" + uri.getSlug() + "/services";
        String apiUrl = uri.getApiUrl();
        // Find the hook
        try
        {
            String responseString = communicatorHelper.get(auth, urlPath, null, apiUrl);
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
                    communicatorHelper.delete(auth, apiUrl, urlPath + "/" + id);
                }
            }
        } catch (ResponseException e)
        {
            logger.warn("Error removing postcommit service [{}]", e.getMessage());
        } catch (JSONException e)
        {
            logger.warn("Error removing postcommit service [{}]", e.getMessage());
        }
    }

    @Override
    public Iterable<Changeset> getChangesets(final SourceControlRepository repository)
    {
        return new Iterable<Changeset>()
        {
            @Override
            public Iterator<Changeset> iterator()
            {
                return new BitbucketChangesetIterator(BitbucketCommunicator.this, repository);
            }
        };
    }
    
    @Override
    public UrlInfo getUrlInfo(final RepositoryUri repositoryUri)
    {
        logger.debug("get repository info in bitbucket [ {} ]", repositoryUri.getRepositoryUrl());
        Boolean repositoryPrivate = communicatorHelper.isRepositoryPrivate1(repositoryUri);
        if (repositoryPrivate == null) return null;
        return new UrlInfo(BitbucketRepositoryManager.BITBUCKET, repositoryPrivate.booleanValue());
    }
    
}
