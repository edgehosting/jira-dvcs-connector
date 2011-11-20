package com.atlassian.jira.plugins.bitbucket.spi.github.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.bitbucket.api.Authentication;
import com.atlassian.jira.plugins.bitbucket.api.AuthenticationFactory;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlUser;
import com.atlassian.jira.plugins.bitbucket.spi.Communicator;
import com.atlassian.jira.plugins.bitbucket.spi.CommunicatorHelper;
import com.atlassian.jira.plugins.bitbucket.spi.CustomStringUtils;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.spi.UrlInfo;
import com.atlassian.jira.plugins.bitbucket.spi.github.GithubChangesetFactory;
import com.atlassian.jira.plugins.bitbucket.spi.github.GithubUserFactory;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.net.RequestFactory;
import com.atlassian.sal.api.net.ResponseException;

public class GithubCommunicator implements Communicator
{
    private final Logger logger = LoggerFactory.getLogger(GithubCommunicator.class);

    private final AuthenticationFactory authenticationFactory;
    private final CommunicatorHelper communicatorHelper;

    public GithubCommunicator(RequestFactory<?> requestFactory, AuthenticationFactory authenticationFactory)
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

            String responseString = communicatorHelper.get(Authentication.ANONYMOUS, "/user/show/" + CustomStringUtils.encode(username), null, uri.getApiUrl());
            return GithubUserFactory.parse(new JSONObject(responseString).getJSONObject("user"));
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
            // TODO: oAuth
//            Authentication auth = authenticationFactory.getAuthentication(repository);

            logger.debug("parse gihchangeset [ {} ] [ {} ] [ {} ]", new String[] { owner, slug, id });
            String responseString = communicatorHelper.get(Authentication.ANONYMOUS, "/commits/show/" + CustomStringUtils.encode(owner) + "/" +
                    CustomStringUtils.encode(slug) + "/" + CustomStringUtils.encode(id), null,
                    uri.getApiUrl());

            // TODO; branch
            return GithubChangesetFactory.parse(repository.getId(), "master", new JSONObject(responseString).getJSONObject("commit"));
        } catch (ResponseException e)
        {
            throw new SourceControlException("could not get result", e);
        } catch (JSONException e)
        {
            throw new SourceControlException("could not parse json result", e);
        }

    }

    public List<Changeset> getChangesets(SourceControlRepository repository, String branch, int pageNumber)
    {
        RepositoryUri uri = repository.getRepositoryUri();
        String owner = uri.getOwner();
        String slug = uri.getSlug();
        // TODO: oAuth
        // Authentication auth = authenticationFactory.getAuthentication(repository);

        logger.debug("parse github changesets [ {} ] [ {} ] [ {} ]", new String[] { owner, slug, String.valueOf(pageNumber) });

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("page", String.valueOf(pageNumber));

        List<Changeset> changesets = new ArrayList<Changeset>();

        String responseString = null;
        try
        {
            responseString = communicatorHelper.get(Authentication.ANONYMOUS, "/commits/list/" + CustomStringUtils.encode(owner) + "/" +
                    CustomStringUtils.encode(slug) + "/master", params, uri.getApiUrl());
        } catch (ResponseException e)
        {
            logger.debug("Could not get changesets from page: {}", pageNumber);
            // ResponseException should really provide the error code :(
            if ("Unexpected response received. Status code: 401".equals(e.getMessage()))
            {
                throw new SourceControlException("Incorrect credentials");
            }
            throw new SourceControlException("Error requesting changesets. Page: "+pageNumber + ". ["+e.getMessage()+"]", e);
        }

        try
        {
            JSONArray list = new JSONObject(responseString).getJSONArray("commits");
            for (int i = 0; i < list.length(); i++)
            {
                changesets.add(GithubChangesetFactory.parse(repository.getId(), branch, list.getJSONObject(i)));
            }
        } catch (JSONException e)
        {
            logger.debug("Could not parse repositories from page: {}", pageNumber);
            return Collections.emptyList();
        }

        return changesets;

    }

    @Override
    public void setupPostcommitHook(SourceControlRepository repo, String postCommitUrl)
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public void removePostcommitHook(SourceControlRepository repo, String postCommitUrl)
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public Iterable<Changeset> getChangesets(final SourceControlRepository repository)
    {
        return new Iterable<Changeset>()
        {
            @Override
            public Iterator<Changeset> iterator()
            {
                return new GithubChangesetIterator(GithubCommunicator.this, repository);
            }
        };
    }

    @Override
    public UrlInfo getUrlInfo(final RepositoryUri repositoryUri)
    {
        logger.debug("get repository info in bitbucket [ {} ]", repositoryUri.getRepositoryUrl());
        Boolean repositoryPrivate = communicatorHelper.isRepositoryPrivate(repositoryUri);
        if (repositoryPrivate == null) return null;
        return new UrlInfo(GithubRepositoryManager.GITHUB, repositoryPrivate.booleanValue());
    }
}
