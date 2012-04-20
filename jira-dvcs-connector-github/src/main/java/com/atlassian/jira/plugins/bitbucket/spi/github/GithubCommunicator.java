package com.atlassian.jira.plugins.bitbucket.spi.github;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.bitbucket.api.Authentication;
import com.atlassian.jira.plugins.bitbucket.api.AuthenticationFactory;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.Communicator;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryPersister;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlUser;
import com.atlassian.jira.plugins.bitbucket.api.exception.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.impl.GithubOAuthAuthentication;
import com.atlassian.jira.plugins.bitbucket.api.net.ExtendedResponseHandler.ExtendedResponse;
import com.atlassian.jira.plugins.bitbucket.api.net.RequestHelper;
import com.atlassian.jira.plugins.bitbucket.api.rest.UrlInfo;
import com.atlassian.jira.plugins.bitbucket.api.util.CustomStringUtils;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.net.ResponseException;

public class GithubCommunicator implements Communicator
{
    private final Logger log = LoggerFactory.getLogger(GithubCommunicator.class);

    private final AuthenticationFactory authenticationFactory;
    private final RequestHelper requestHelper;

    private final RepositoryPersister repositoryPersister;

    public GithubCommunicator(AuthenticationFactory authenticationFactory, RequestHelper requestHelper, RepositoryPersister repositoryPersister)
    {
        this.authenticationFactory = authenticationFactory;
        this.requestHelper = requestHelper;
        this.repositoryPersister = repositoryPersister;
    }

    @Override
    public SourceControlUser getUser(SourceControlRepository repository, String username)
    {
        try
        {
            RepositoryUri uri = repository.getRepositoryUri();
            log.debug("parse user [ {} ]", username);
            Authentication authentication = authenticationFactory.getAuthentication(repository);
            String responseString = requestHelper.get(authentication, "/user/show/" + CustomStringUtils.encode(username), null, uri.getApiUrl());
            return GithubUserFactory.parse(new JSONObject(responseString).getJSONObject("user"));
        } catch (ResponseException e)
        {
            log.debug("could not load user [ " + username + " ]");
            return SourceControlUser.UNKNOWN_USER;
        } catch (JSONException e)
        {
            log.debug("could not load user [ " + username + " ]");
            return SourceControlUser.UNKNOWN_USER;
        }
    }

    @Override
    public Changeset getChangeset(SourceControlRepository repository, String id)
    {
        try
        {
            String apiUrl = "https://api.github.com"; // we have to use API v3
            RepositoryUri uri = repository.getRepositoryUri();
            String owner = uri.getOwner();
            String slug = uri.getSlug();
            Authentication authentication = authenticationFactory.getAuthentication(repository);

            log.debug("parse gihchangeset [ {} ] [ {} ] [ {} ]", new String[]{owner, slug, id});
            String responseString = requestHelper.get(authentication, "/repos/" + CustomStringUtils.encode(owner) + "/" +
                    CustomStringUtils.encode(slug) + "/commits/" + CustomStringUtils.encode(id), null,
                    apiUrl);

            return GithubChangesetFactory.parseV3(repository.getId(), "master", new JSONObject(responseString));
        } catch (ResponseException e)
        {
            throw new SourceControlException("could not get result", e);
        } catch (JSONException e)
        {
            throw new SourceControlException("could not parse json result", e);
        }
    }

    @Override
    public Changeset getChangeset(SourceControlRepository repository, Changeset changeset)
    {
        final Changeset reloadedChangeset = getChangeset(repository, changeset.getNode());
        if (StringUtils.isNotBlank(changeset.getBranch()))
        {
            reloadedChangeset.setBranch(changeset.getBranch());
        }
        return reloadedChangeset;
    }

    public List<Changeset> getChangesets(SourceControlRepository repository, String branch, int pageNumber)
    {
        RepositoryUri uri = repository.getRepositoryUri();
        String owner = uri.getOwner();
        String slug = uri.getSlug();
        Authentication authentication = authenticationFactory.getAuthentication(repository);

        log.debug("parse github changesets [ {} ] [ {} ] [ {} ]", new String[]{owner, slug, String.valueOf(pageNumber)});

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("page", String.valueOf(pageNumber));

        List<Changeset> changesets = new ArrayList<Changeset>();

        try
        {
            ExtendedResponse extendedResponse = requestHelper.getExtendedResponse(authentication, "/commits/list/" + CustomStringUtils.encode(owner) + "/" +
                    CustomStringUtils.encode(slug) + "/" + branch, params, uri.getApiUrl());

            if (extendedResponse.getStatusCode() == HttpStatus.SC_UNAUTHORIZED)
            {
                throw new SourceControlException("Incorrect credentials");
            } else if (extendedResponse.getStatusCode() == HttpStatus.SC_NOT_FOUND)
            {
                // no more changesets
                log.debug("Page: {} not contains changesets. Return empty list", pageNumber);
                return Collections.emptyList();
            }

            if (extendedResponse.isSuccessful())
            {
                String responseString = extendedResponse.getResponseString();
                JSONArray list = new JSONObject(responseString).getJSONArray("commits");
                for (int i = 0; i < list.length(); i++)
                {
                    JSONObject commitJson = list.getJSONObject(i);
                    final Changeset changeset = GithubChangesetFactory.parseV2(repository.getId(), commitJson);
                    changeset.setBranch(branch);
                    changesets.add(changeset);
                }
            } else
            {
                throw new ResponseException("Server response was not successful! Http Status Code: " + extendedResponse.getStatusCode());
            }
        } catch (ResponseException e)
        {
            log.debug("Could not get changesets from page: {}", pageNumber, e);
            throw new SourceControlException("Error requesting changesets. Page: " + pageNumber + ". [" + e.getMessage() + "]", e);
        } catch (JSONException e)
        {
            log.debug("Could not parse repositories from page: {}", pageNumber);
            return Collections.emptyList();
        }

        return changesets;
    }

    @Override
    public void setupPostcommitHook(SourceControlRepository repo, String postCommitUrl)
    {
        RepositoryUri uri = repo.getRepositoryUri();
        Authentication auth = authenticationFactory.getAuthentication(repo);

        String urlPath = "/repos/" + uri.getOwner() + "/" + uri.getSlug() + "/hooks";
        String apiUrl = "https://api.github.com"; // we have to use API v3

        try
        {
            JSONObject configJson = new JSONObject().put("url", postCommitUrl);
            JSONObject postDataJson = new JSONObject().put("name", "web").put("active", true).put("config", configJson);
            requestHelper.post(auth, urlPath, postDataJson.toString(), apiUrl);
        } catch (JSONException e)
        {
            throw new SourceControlException("Could not create relevant POST data for postcommit hook.", e);
        } catch (ResponseException e)
        {
            throw new SourceControlException("Could not add postcommit hook. ", e);
        }
    }

    @Override
    public void removePostcommitHook(SourceControlRepository repo, String postCommitUrl)
    {
        RepositoryUri uri = repo.getRepositoryUri();
        Authentication auth = authenticationFactory.getAuthentication(repo);
        String urlPath = "/repos/" + uri.getOwner() + "/" + uri.getSlug() + "/hooks";
        String apiUrl = "https://api.github.com"; // hardcoded url because we have to use API v3
        // Find the hook
        try
        {
            String responseString = requestHelper.get(auth, urlPath, null, apiUrl);
            JSONArray jsonArray = new JSONArray(responseString);
            for (int i = 0; i < jsonArray.length(); i++)
            {
                JSONObject data = (JSONObject) jsonArray.get(i);
                String id = data.getString("id");
                JSONObject config = data.getJSONObject("config");
                String url = config.getString("url");
                if (postCommitUrl.equals(url))
                {
                    // We have the hook, lets remove it
                    requestHelper.delete(auth, apiUrl, urlPath + "/" + id);
                }
            }
        } catch (ResponseException e)
        {
            log.warn("Error removing postcommit service [{}]", e.getMessage());
        } catch (JSONException e)
        {
            log.warn("Error removing postcommit service [{}]", e.getMessage());
        }
    }

    @Override
    public Iterable<Changeset> getChangesets(final SourceControlRepository repository, final Date lastCommitDate)
    {
        return new Iterable<Changeset>()
        {
            @Override
            public Iterator<Changeset> iterator()
            {
                List<String> branches = getBranches(repository);
                return new GithubChangesetIterator(repositoryPersister, GithubCommunicator.this, repository, branches, lastCommitDate);
            }
        };
    }

    @Override
    public UrlInfo getUrlInfo(final RepositoryUri repositoryUri, String projectKey)
    {
        log.debug("get repository info in bitbucket [ {} ]", repositoryUri.getRepositoryUrl());
        Boolean repositoryPrivate = requestHelper.isRepositoryPrivate1(repositoryUri);
        if (repositoryPrivate == null) return null;
        return new UrlInfo(GithubRepositoryManager.GITHUB, repositoryPrivate.booleanValue(), repositoryUri.getRepositoryUrl(), projectKey);
    }

    private List<String> getBranches(SourceControlRepository repository)
    {
        List<String> branches = new ArrayList<String>();
        RepositoryUri repositoryUri = repository.getRepositoryUri();
        String owner = repositoryUri.getOwner();
        String slug = repositoryUri.getSlug();
        Authentication authentication = authenticationFactory.getAuthentication(repository);

        log.debug("get list of branches in github repository [ {} ]", slug);

        try
        {
            String responseString = requestHelper.get(authentication, "/repos/show/" +
                    CustomStringUtils.encode(owner) + "/" + CustomStringUtils.encode(slug) + "/branches", null, repositoryUri.getApiUrl());

            JSONArray list = new JSONObject(responseString).getJSONObject("branches").names();
            for (int i = 0; i < list.length(); i++)
            {
                final String branchName = list.getString(i);
                if (branchName.equalsIgnoreCase("master"))
                {
                    branches.add(0,branchName);
                } else
                {
                    branches.add(branchName);
                }
            }
        } catch (Exception e)
        {
            log.info("Can not obtain branches list from repository [ {} ]", slug);
            // we have to use at least master branch
            return Arrays.asList("master");
        }

        return branches;

    }

    @Override
    public String getRepositoryName(String repositoryType, String projectKey, RepositoryUri repositoryUri,
                                    String adminUsername, String adminPassword, String accessToken) throws SourceControlException
    {
        Authentication auth;
        if (StringUtils.isNotBlank(accessToken))
        {
            auth = new GithubOAuthAuthentication(accessToken);
        } else
        {
            auth = Authentication.ANONYMOUS;
        }

        try
        {
            ExtendedResponse extendedResponse = requestHelper.getExtendedResponse(auth, repositoryUri.getRepositoryInfoUrl(), null,
                    repositoryUri.getApiUrl());
            // in case we have valid access_token but for other account github
            // returns HttpStatus.SC_NOT_FOUND response
            if (extendedResponse.getStatusCode() == HttpStatus.SC_UNAUTHORIZED)
            {
                throw new SourceControlException.UnauthorisedException("You don't have access to the repository.");
            }
            if (extendedResponse.isSuccessful())
            {
                String responseString = extendedResponse.getResponseString();
                return new JSONObject(responseString).getJSONObject("repository").getString("name");
            } else
            {
                throw new ResponseException("Server response was not successful! Http Status Code: " + extendedResponse.getStatusCode());
            }
        } catch (ResponseException e)
        {
            log.debug(e.getMessage(), e);
            throw new SourceControlException(e.getMessage());
        } catch (JSONException e)
        {
            log.debug(e.getMessage(), e);
            throw new SourceControlException(e.getMessage());
        }

    }
}
