package com.atlassian.jira.plugins.bitbucket.spi.github;

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
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.net.ResponseException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.eclipse.egit.github.core.RepositoryBranch;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryHook;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GithubCommunicator implements Communicator
{
    private static final int NUM_ATTEMPTS = 8;

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

    private GitHubClient createClient(SourceControlRepository repository)
    {
        final RepositoryUri uri = repository.getRepositoryUri();
        String host = uri.getBaseUrl();
        GithubOAuthAuthentication auth = (GithubOAuthAuthentication) authenticationFactory.getAuthentication(repository);

        final GitHubClient client = GitHubClient.createClient(host);
        client.setOAuth2Token(auth.getAccessToken());
        return client;
    }

    @Override
    public SourceControlUser getUser(SourceControlRepository repository, String username)
    {
        final GitHubClient client = createClient(repository);
        final UserService userService = new UserService(client);

        try
        {
            log.debug("Get user information for: [ {} ]", username);
            final User ghUser = userService.getUser(username);
            return GithubUserFactory.transform(ghUser);
        } catch (IOException e)
        {
            log.debug("could not load user [ " + username + " ]");
            return SourceControlUser.UNKNOWN_USER;
        }
    }

    @Override
    public Changeset getChangeset(SourceControlRepository repository, String id)
    {
        RepositoryUri uri = repository.getRepositoryUri();
        String owner = uri.getOwner();
        String slug = uri.getSlug();

        final GitHubClient client = createClient(repository);
        final CommitService commitService = new CommitService(client);

        try
        {
            final RepositoryCommit commit = commitService.getCommit(RepositoryId.create(owner, slug), id);
            return GithubChangesetFactory.transform(commit, repository.getId(), "master");
        } catch (IOException e)
        {
            throw new SourceControlException("could not get result", e);
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

    public PageIterator getPageIterator(SourceControlRepository repository, String branch)
    {
        for (int attempt = 1; attempt < NUM_ATTEMPTS; attempt++)
        {
            try
            {
                return getPageIteratorInternal(repository, branch);
            } catch (SourceControlException e)
            {
                long delay = (long) (1000*Math.pow(3, attempt));
                log.warn("Attempt #"+attempt+" (out of "+NUM_ATTEMPTS+"): Retrieving changesets failed: " + e.getMessage() +"\n. Retrying in " + delay/1000 + "secs");
                try
                {
                    Thread.sleep(delay);
                } catch (InterruptedException e1)
                {
                    // ignore
                }
            }
        }
        return getPageIteratorInternal(repository, branch);
    }

    private PageIterator getPageIteratorInternal(SourceControlRepository repository, String branch)
    {
        RepositoryUri uri = repository.getRepositoryUri();
        String owner = uri.getOwner();
        String slug = uri.getSlug();

        final GitHubClient client = createClient(repository);
        final CommitService commitService = new CommitService(client);

        return commitService.pageCommits(RepositoryId.create(owner, slug), branch, null);
    }

    @Override
    public void setupPostcommitHook(SourceControlRepository repo, String postCommitUrl)
    {
        final RepositoryUri uri = repo.getRepositoryUri();
        String owner = uri.getOwner();
        String slug = uri.getSlug();

        final GitHubClient client = createClient(repo);
        RepositoryService repositoryService = new RepositoryService(client);

        final RepositoryHook repositoryHook = new RepositoryHook();
        repositoryHook.setName("web");
        repositoryHook.setActive(true);

        Map<String, String> config = new HashMap<String, String>();
        config.put("url", postCommitUrl);
        repositoryHook.setConfig(config);

        try
        {
            repositoryService.createHook(RepositoryId.create(owner, slug), repositoryHook);
        } catch (IOException e)
        {
            throw new SourceControlException("Could not add postcommit hook. ", e);
        }
    }

    @Override
    public void removePostcommitHook(SourceControlRepository repo, String postCommitUrl)
    {
        final RepositoryUri uri = repo.getRepositoryUri();
        String owner = uri.getOwner();
        String slug = uri.getSlug();

        final GitHubClient client = createClient(repo);
        RepositoryService repositoryService = new RepositoryService(client);

        final RepositoryId repositoryId = RepositoryId.create(owner, slug);
        try
        {
            final List<RepositoryHook> hooks = repositoryService.getHooks(repositoryId);
            for (RepositoryHook hook : hooks)
            {
                if (postCommitUrl.equals(hook.getConfig().get("url")))
                {
                    repositoryService.deleteHook(repositoryId, (int) hook.getId());
                }
            }
        } catch (IOException e)
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
        if (repositoryPrivate == null)
        {
            if ("https://github.com".equalsIgnoreCase(repositoryUri.getBaseUrl()) && StringUtils.isNotBlank(repositoryUri.getOwner()) && StringUtils.isNotBlank(repositoryUri.getSlug()))
            {
                repositoryPrivate = Boolean.TRUE;   // it looks like github repository, but github doesn't tell us if it exists. Lets assume it's private
            } else
            {
                return null;
            }
        }
        return new UrlInfo(GithubRepositoryManager.GITHUB, repositoryPrivate.booleanValue(),
                repositoryUri.getRepositoryUrl(), projectKey);
    }


    private List<String> getBranches(SourceControlRepository repository)
    {
        final RepositoryUri uri = repository.getRepositoryUri();
        String owner = uri.getOwner();
        String slug = uri.getSlug();

        final GitHubClient client = createClient(repository);
        RepositoryService repositoryService = new RepositoryService(client);

        List<String> branches = new ArrayList<String>();
        try
        {
            final List<RepositoryBranch> ghBranches = repositoryService.getBranches(RepositoryId.create(owner, slug));
            log.debug("Found branches: " + ghBranches.size());

            for (RepositoryBranch ghBranch: ghBranches)
            {
                final String branchName = ghBranch.getName();
                if (branchName.equalsIgnoreCase("master"))
                {
                    branches.add(0,branchName);
                } else
                {
                    branches.add(branchName);
                }
            }

        } catch (IOException e)
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

            // for unauthorized access Github API v3 return 404
            if (extendedResponse.getStatusCode() == HttpStatus.SC_NOT_FOUND)
            {
                throw new SourceControlException.UnauthorisedException("You don't have access to the repository.");
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
            log.debug(e.getMessage(), e);
            throw new SourceControlException(e.getMessage());
        } catch (JSONException e)
        {
            log.debug(e.getMessage(), e);
            throw new SourceControlException(e.getMessage());
        }

    }
}
