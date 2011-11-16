package com.atlassian.jira.plugins.bitbucket.spi.github.impl;

import com.atlassian.jira.plugins.bitbucket.api.Authentication;
import com.atlassian.jira.plugins.bitbucket.api.AuthenticationFactory;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlUser;
import com.atlassian.jira.plugins.bitbucket.spi.Communicator;
import com.atlassian.jira.plugins.bitbucket.spi.CommunicatorHelper;
import com.atlassian.jira.plugins.bitbucket.spi.CustomStringUtils;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.spi.github.GithubChangesetFactory;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.net.RequestFactory;
import com.atlassian.sal.api.net.ResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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


    public SourceControlUser getUser(SourceControlRepository repository, String username)
    {
        return null;
    }

    public Changeset getChangeset(SourceControlRepository repository, String id)
    {
        return null;
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
            logger.debug("Could not get repositories from page: {}", pageNumber);
            return Collections.emptyList();
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

    public void setupPostcommitHook(SourceControlRepository repo, String postCommitUrl)
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void removePostcommitHook(SourceControlRepository repo, String postCommitUrl)
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public Iterable<Changeset> getChangesets(final SourceControlRepository repository)
    {
        return new Iterable<Changeset>()
        {
            public Iterator<Changeset> iterator()
            {
                return new GithubChangesetIterator(GithubCommunicator.this, repository);
            }
        };
    }

    public boolean isRepositoryValid(RepositoryUri repositoryUri)
    {
        String owner = repositoryUri.getOwner();
        String slug = repositoryUri.getSlug();

        logger.debug("get repository info in github [ {} ]", slug);

        try
        {
            String responseString = communicatorHelper.get(Authentication.ANONYMOUS, "/repos/show/" +
                    CustomStringUtils.encode(owner) + "/" + CustomStringUtils.encode(slug), null, repositoryUri.getApiUrl());

            String name = new JSONObject(responseString).getJSONObject("repository").getString("name");
            if (name.equalsIgnoreCase(repositoryUri.getSlug()))
            {
                return true;
            }
        } catch (Exception e)
        {
            logger.info("Not github repository [ {} ]", slug);
            return false;
        }

        return false;

    }
}
