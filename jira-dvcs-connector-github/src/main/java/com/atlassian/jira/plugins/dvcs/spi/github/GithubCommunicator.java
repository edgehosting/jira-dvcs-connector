package com.atlassian.jira.plugins.dvcs.spi.github;

import com.atlassian.jira.plugins.dvcs.auth.Authentication;
import com.atlassian.jira.plugins.dvcs.auth.AuthenticationFactory;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.net.ExtendedResponseHandler;
import com.atlassian.jira.plugins.dvcs.net.RequestHelper;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.github.parsers.GithubChangesetFactory;
import com.atlassian.jira.plugins.dvcs.spi.github.parsers.GithubUserFactory;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.net.ResponseException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GithubCommunicator implements DvcsCommunicator
{
    private static final Logger log = LoggerFactory.getLogger(GithubCommunicator.class);

    public static final String GITHUB = "github";

    private ChangesetService changesetService;
    private RequestHelper requestHelper;
    private AuthenticationFactory authenticationFactory;
    private GithubOAuth githubOAuth;

    public GithubCommunicator()
    {
    }

    @Override
	public boolean isOauthConfigured()
	{
		return StringUtils.isNotBlank(githubOAuth.getClientId()) 
				&& StringUtils.isNotBlank(githubOAuth.getClientSecret());
	}

	
    @Override
    public String getDvcsType()
    {
        return GITHUB;
    }
    

    @Override
    public AccountInfo getAccountInfo(String hostUrl, String accountName)
    {
        UserService userService = new UserService(GitHubClient.createClient(hostUrl));
        try
        {
        	// TODO not used, delete if no need
            User user = userService.getUser(accountName);
            
            boolean requiresOauth = StringUtils.isBlank(githubOAuth.getClientId()) || StringUtils.isBlank(githubOAuth.getClientSecret());

            return new AccountInfo(GithubCommunicator.GITHUB, requiresOauth);
            
        } catch (IOException e)
        {
            log.debug("Unable to retrieve account information ", e);
        }
        return null;

    }

    @Override
    public List<Repository> getRepositories(Organization organization)
    {
        RepositoryService repositoryService = new RepositoryService(GitHubClient.createClient(organization.getHostUrl()));
        repositoryService.getClient().setOAuth2Token(organization.getCredential().getAccessToken());
        try
        {
            List<org.eclipse.egit.github.core.Repository> ghRepositories = repositoryService.getRepositories(organization.getName());
            List<Repository> repositories = new ArrayList<Repository>();
            for (org.eclipse.egit.github.core.Repository ghRepository : ghRepositories)
            {
                Repository repository = new Repository();
                repository.setSlug(ghRepository.getName());
                repository.setName(ghRepository.getName());
                repositories.add(repository);
            }
            log.debug("Found repositories: " + repositories.size());
            return repositories;
        } catch (IOException e)
        {
            throw new SourceControlException("Error retrieving list of repositories", e);
        }

    }

	@Override
	public Changeset getDetailChangeset(Repository repository, Changeset changeset)
	{
        try
        {
            String apiUrl = getApiUrl(repository.getOrgHostUrl(), true);
            String owner = repository.getOrgName();
            String slug = repository.getSlug();
            String node = changeset.getNode();
            Authentication authentication = authenticationFactory.getAuthentication(repository);

            log.debug("parse gihchangeset [ {} ] [ {} ] [ {} ]", new String[]{owner, slug, node});
            String responseString = requestHelper.get(authentication, "/repos/" + CustomStringUtils.encode(owner) + "/" +
                    CustomStringUtils.encode(slug) + "/commits/" + CustomStringUtils.encode(node), null,
                    apiUrl);

            final Changeset detailChangeset = GithubChangesetFactory.parseV3(repository.getId(), "master", new JSONObject(responseString));

            if (StringUtils.isNotBlank(changeset.getBranch()))
            {
                detailChangeset.setBranch(changeset.getBranch());
            }

            return detailChangeset;
        } catch (ResponseException e)
        {
            throw new SourceControlException("could not get result", e);
        } catch (JSONException e)
        {
            throw new SourceControlException("could not parse json result", e);
        }

	}

    @Override
    public void setupPostcommitHook(Repository repository, String postCommitUrl)
    {
        String apiUrl = getApiUrl(repository.getOrgHostUrl(), true);
        String owner = repository.getOrgName();
        String slug = repository.getSlug();
        Authentication auth = authenticationFactory.getAuthentication(repository);

        String urlPath = "/repos/" + owner + "/" + slug + "/hooks";

        try
        {
            JSONObject configJson = new JSONObject().put("url", postCommitUrl);
            JSONObject postDataJson = new JSONObject().put("name", "web").put("active", true).put("config", configJson);
            requestHelper.post(auth, urlPath, postDataJson.toString(), apiUrl);
        } catch (JSONException e)
        {
        	log.warn("Error installing postcommit service [{}]", e.getMessage());
            throw new SourceControlException("Could not create relevant POST data for postcommit hook.", e);
        } catch (ResponseException e)
        {
        	log.warn("Error installing postcommit service [{}]", e.getMessage());
            throw new SourceControlException("Could not add postcommit hook. ", e);
        }
    }

    @Override
    public Iterable<Changeset> getChangesets(final Repository repository, final Date lastCommitDate)
    {
        return new Iterable<Changeset>()
        {
            @Override
            public Iterator<Changeset> iterator()
            {
                List<String> branches = getBranches(repository);
                return new GithubChangesetIterator(changesetService, GithubCommunicator.this, repository, branches, lastCommitDate);
            }
        };
    }

    @Override
    public void removePostcommitHook(Repository repository, String postCommitUrl)
    {
        String apiUrl = getApiUrl(repository.getOrgHostUrl(), true);
        String owner = repository.getOrgName();
        String slug = repository.getSlug();
        Authentication auth = authenticationFactory.getAuthentication(repository);

        String urlPath = "/repos/" + owner + "/" + slug + "/hooks";
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
            throw new SourceControlException(e);
        } catch (JSONException e)
        {
            log.warn("Error removing postcommit service [{}]", e.getMessage());
            throw new SourceControlException(e);
        }
    }

    @Override
    public String getCommitUrl(Repository repository, Changeset changeset)
    {
        return MessageFormat.format("{0}/{1}/{2}/commit/{3}", repository.getOrgHostUrl(),
                        repository.getOrgName(), repository.getSlug(), changeset.getNode());
    }

    @Override
    public String getFileCommitUrl(Repository repository, Changeset changeset, String file, int index)
    {
        return MessageFormat.format("{0}#diff-{1}", getCommitUrl(repository, changeset), index);
    }

    @Override
    public DvcsUser getUser(Repository repository, String username)
    {
        try
        {
            String apiUrl = getApiUrl(repository.getOrgHostUrl(), false);

            log.debug("parse user [ {} ]", username);
            Authentication authentication = authenticationFactory.getAuthentication(repository);
            String responseString = requestHelper.get(authentication, "/user/show/" + CustomStringUtils.encode(username), null, apiUrl);
            return GithubUserFactory.parse(new JSONObject(responseString).getJSONObject("user"));
        } catch (ResponseException e)
        {
            log.debug("could not load user [ " + username + " ]");
            return DvcsUser.UNKNOWN_USER;
        } catch (JSONException e)
        {
            log.debug("could not load user [ " + username + " ]");
            return DvcsUser.UNKNOWN_USER;
        }

    }

    @Override
    public String getUserUrl(Repository repository, Changeset changeset)
    {
        return MessageFormat.format("{0}/{1}", repository.getOrgHostUrl(), changeset.getAuthor());
    }


    private String getApiUrl(String hostUrl, boolean v3) {

        if (v3) {
            // todo: what URL is for v3 Api when server is not github.com?
            return "https://api.github.com";
        }

        return hostUrl + "/api/v2/json";
    }

    private List<String> getBranches(Repository repository)
    {
        List<String> branches = new ArrayList<String>();

        String apiUrl = getApiUrl(repository.getOrgHostUrl(), false);
        String owner = repository.getOrgName();
        String slug = repository.getSlug();
        Authentication authentication = authenticationFactory.getAuthentication(repository);

        log.debug("get list of branches in github repository [ {} ]", slug);

        try
        {
            String responseString = requestHelper.get(authentication, "/repos/show/" +
                    CustomStringUtils.encode(owner) + "/" + CustomStringUtils.encode(slug) + "/branches", null, apiUrl);

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

    public List<Changeset> getChangesets(Repository repository, String branch, int pageNumber)
    {
        String apiUrl = getApiUrl(repository.getOrgHostUrl(), false);
        String owner = repository.getOrgName();
        String slug = repository.getSlug();
        Authentication authentication = authenticationFactory.getAuthentication(repository);

        log.debug("parse github changesets [ {} ] [ {} ] [ {} ]", new String[]{owner, slug, String.valueOf(pageNumber)});

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("page", String.valueOf(pageNumber));

        List<Changeset> changesets = new ArrayList<Changeset>();

        try
        {
            ExtendedResponseHandler.ExtendedResponse extendedResponse = requestHelper.getExtendedResponse(authentication, "/commits/list/" + CustomStringUtils.encode(owner) + "/" +
                    CustomStringUtils.encode(slug) + "/" + branch, params, apiUrl);

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
            throw new SourceControlException(e);
        }

        return changesets;
    }

	@Override
	public boolean validateCredentials(Organization organization)
	{
		return true;
	}

	@Override
	public boolean supportsInvitation(Organization organization)
	{
		return false;
	}

	@Override
	public List<Group> getGroupsForOrganization(Organization organization)
	{
		return Collections.emptyList();
	}

	@Override
	public void inviteUser(Organization organization, Collection<String> groupSlugs, String userEmail)
	{
		throw new UnsupportedOperationException("You can not invite users to github so far, ...");
	}

	public void setChangesetService(ChangesetService changesetService)
    {
        this.changesetService = changesetService;
    }

    public void setRequestHelper(RequestHelper requestHelper)
    {
        this.requestHelper = requestHelper;
    }

    public void setAuthenticationFactory(AuthenticationFactory authenticationFactory)
    {
        this.authenticationFactory = authenticationFactory;
    }

    public void setGithubOAuth(GithubOAuth githubOAuth)
    {
        this.githubOAuth = githubOAuth;
    }

    
}
