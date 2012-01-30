package com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl;

import com.atlassian.jira.plugins.bitbucket.api.*;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException.UnauthorisedException;
import com.atlassian.jira.plugins.bitbucket.api.impl.BasicAuthentication;
import com.atlassian.jira.plugins.bitbucket.spi.*;
import com.atlassian.jira.plugins.bitbucket.spi.ExtendedResponseHandler.ExtendedResponse;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketChangesetFactory;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketUserFactory;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.net.ResponseException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Starting point for remote API calls to the bitbucket remote API
 */
public class BitbucketCommunicator implements Communicator
{
    private final Logger logger = LoggerFactory.getLogger(BitbucketCommunicator.class);

    private final AuthenticationFactory authenticationFactory;
    private final RequestHelper requestHelper;

    public BitbucketCommunicator(AuthenticationFactory authenticationFactory, RequestHelper requestHelper)
    {
        this.authenticationFactory = authenticationFactory;
        this.requestHelper = requestHelper;
    }

    @Override
    public SourceControlUser getUser(SourceControlRepository repository, String username)
    {
        try
        {
            RepositoryUri uri = repository.getRepositoryUri();
            logger.debug("Parse user [ {} ]", username);

            String responseString = requestHelper.get(Authentication.ANONYMOUS, "/users/" + CustomStringUtils.encode(username), null, uri.getApiUrl());
            return BitbucketUserFactory.parse(new JSONObject(responseString).getJSONObject("user"));
        } catch (ResponseException e)
        {
            logger.debug("Could not load user [ " + username + " ]");
            return SourceControlUser.UNKNOWN_USER;
        } catch (JSONException e)
        {
            logger.debug("Could not load user [ " + username + " ]");
            return SourceControlUser.UNKNOWN_USER;
        }
    }

    @Override
    public Changeset getChangeset(SourceControlRepository repository, String node)
    {
        try
        {
            RepositoryUri uri = repository.getRepositoryUri();
            String owner = uri.getOwner();
            String slug = uri.getSlug();
            Authentication auth = authenticationFactory.getAuthentication(repository);

            logger.debug("Parse changeset [ {} ] [ {} ] [ {} ]", new String[]{owner, slug, node});
            final String urlPath = "/repositories/" + CustomStringUtils.encode(owner) + "/" +
                    CustomStringUtils.encode(slug) + "/changesets/" + CustomStringUtils.encode(node);
            String responseString = requestHelper.get(auth, urlPath, null, uri.getApiUrl());
            String responseFilesString = requestHelper.get(auth, urlPath + "/diffstat", null, uri.getApiUrl());
            return BitbucketChangesetFactory.parse(repository.getId(), new JSONObject(responseString), new JSONArray(responseFilesString));

        } catch (ResponseException e)
        {
            throw new SourceControlException("Could not get result", e);
        } catch (JSONException e)
        {
            throw new SourceControlException("Could not parse json result", e);
        }
    }

    public List<Changeset> getChangesets(final SourceControlRepository repository, String startNode, int limit)
    {
        RepositoryUri uri = repository.getRepositoryUri();
        String owner = uri.getOwner();
        String slug = uri.getSlug();
        Authentication auth = authenticationFactory.getAuthentication(repository);

        logger.debug("Parse bitbucket changesets [ {} ] [ {} ] [ {} ] [ {} ]", new String[]{owner, slug, startNode, String.valueOf(limit)});
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("limit", String.valueOf(limit));
        if (startNode != null)
        {
            params.put("start", startNode);
        }

        List<Changeset> changesets = new ArrayList<Changeset>();

        try
        {
            ExtendedResponse extendedResponse = requestHelper.getExtendedResponse(auth, "/repositories/" + CustomStringUtils.encode(owner)
                    + "/" + CustomStringUtils.encode(slug) + "/changesets", params, uri.getApiUrl());

            if (extendedResponse.getStatusCode() == HttpStatus.SC_UNAUTHORIZED)
            {
                throw new SourceControlException("Incorrect credentials");
            } else if (extendedResponse.getStatusCode() == HttpStatus.SC_NOT_FOUND)
            {
                // no more changesets
                return Collections.emptyList();
            }

            if (extendedResponse.isSuccessful())
            {
                JSONArray list = new JSONObject(extendedResponse.getResponseString()).getJSONArray("changesets");
                for (int i = 0; i < list.length(); i++)
                {
                    JSONObject json = list.getJSONObject(i);
                    changesets.add(BitbucketChangesetFactory.parse(repository.getId(), json, new JSONArray()));
                }
            } else
            {
                throw new ResponseException("Server response was not successful! Http Status Code: " + extendedResponse.getStatusCode());
            }
        } catch (ResponseException e)
        {
            logger.warn("Could not get changesets from node: {}", startNode);
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
            requestHelper.post(auth, urlPath, postData, apiUrl);
        } catch (ResponseException e)
        {
            throw new SourceControlException("Could not add postcommit hook", e);
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
            String responseString = requestHelper.get(auth, urlPath, null, apiUrl);
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
                    requestHelper.delete(auth, apiUrl, urlPath + "/" + id);
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
        logger.debug("Get repository info in bitbucket [ {} ]", repositoryUri.getRepositoryUrl());
        Boolean repositoryPrivate = requestHelper.isRepositoryPrivate1(repositoryUri);
        if (repositoryPrivate == null) return null;
        return new UrlInfo(BitbucketRepositoryManager.BITBUCKET, repositoryPrivate.booleanValue(), null);
    }

    @Override
    public String getRepositoryName(String repositoryType, String projectKey, RepositoryUri repositoryUri,
                                    String adminUsername, String adminPassword, String accessToken) throws SourceControlException
    {

        Authentication auth;
        if (StringUtils.isNotBlank(adminUsername) && StringUtils.isNotBlank(adminUsername))
        {
            auth = new BasicAuthentication(adminUsername, adminUsername);
        } else
        {
            auth = Authentication.ANONYMOUS;
        }

        try
        {
            ExtendedResponse extendedResponse = requestHelper.getExtendedResponse(auth, repositoryUri.getRepositoryInfoUrl(), null, repositoryUri.getApiUrl());
            if (extendedResponse.getStatusCode() == HttpStatus.SC_UNAUTHORIZED)
            {
                throw new UnauthorisedException("Invalid credentials");
            }
            if (extendedResponse.isSuccessful())
            {
                String responseString = extendedResponse.getResponseString();
                return new JSONObject(responseString).getString("name");
            } else
            {
                throw new ResponseException("Server response was not successful! Http Status Code: " + extendedResponse.getStatusCode());
            }
        } catch (ResponseException e)
        {
            logger.debug(e.getMessage(), e);
            throw new SourceControlException(e.getMessage());
        } catch (JSONException e)
        {
            logger.debug(e.getMessage(), e);
            throw new SourceControlException(e.getMessage());
        }
    }

}
