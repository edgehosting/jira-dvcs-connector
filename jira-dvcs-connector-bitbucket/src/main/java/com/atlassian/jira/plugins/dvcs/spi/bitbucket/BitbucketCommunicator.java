package com.atlassian.jira.plugins.dvcs.spi.bitbucket;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.httpclient.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.dvcs.auth.Authentication;
import com.atlassian.jira.plugins.dvcs.auth.AuthenticationFactory;
import com.atlassian.jira.plugins.dvcs.auth.impl.BasicAuthentication;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.net.ExtendedResponseHandler;
import com.atlassian.jira.plugins.dvcs.net.ExtendedResponseHandler.ExtendedResponse;
import com.atlassian.jira.plugins.dvcs.net.RequestHelper;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker.BitbucketLinker;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.parsers.BitbucketChangesetFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.parsers.BitbucketRepositoriesParser;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.parsers.BitbucketUserFactory;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.jira.plugins.dvcs.util.Retryer;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.sal.api.net.Request.MethodType;
import com.atlassian.sal.api.net.ResponseException;

/**
 * The Class BitbucketCommunicator.
 */
public class BitbucketCommunicator implements DvcsCommunicator
{

	/** The Constant log. */
	private static final Logger log = LoggerFactory.getLogger(BitbucketCommunicator.class);

	/** The Constant BITBUCKET. */
	public static final String BITBUCKET = "bitbucket";

	private static final String PLUGIN_KEY = "com.atlassian.jira.plugins.jira-bitbucket-connector-plugin";

	/** The request helper. */
	private final RequestHelper requestHelper;

	/** The authentication factory. */
	private final AuthenticationFactory authenticationFactory;

	private final BitbucketLinker bitbucketLinker;

	private final String pluginVersion;

	/**
	 * The Constructor.
	 * 
	 * @param authenticationFactory
	 *            the authentication factory
	 * @param requestHelper
	 *            the request helper
	 */
	public BitbucketCommunicator(AuthenticationFactory authenticationFactory, RequestHelper requestHelper,
			@Qualifier("defferedBitbucketLinker") BitbucketLinker bitbucketLinker, PluginAccessor pluginAccessor)
	{
		this.authenticationFactory = authenticationFactory;
		this.requestHelper = requestHelper;
		this.bitbucketLinker = bitbucketLinker;
		pluginVersion = getPluginVersion(pluginAccessor);
	}

	protected String getPluginVersion(PluginAccessor pluginAccessor)
	{
		return pluginAccessor.getPlugin(PLUGIN_KEY).getPluginInformation().getVersion();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDvcsType()
	{
		return BITBUCKET;
	}

	/**
	 * Returns always <code>false</code> as we don't support OAuth for BB so far
	 * ...
	 * 
	 * @return true, if checks if is oauth configured
	 */
	@Override
	public boolean isOauthConfigured()
	{
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AccountInfo getAccountInfo(String hostUrl, String accountName)
	{
		String responseString = null;
		try
		{
			String accountUrl = "/users/" + accountName;
			ExtendedResponseHandler.ExtendedResponse extendedResponse = requestHelper.getExtendedResponse(null,
					accountUrl, null, getApiUrl(hostUrl));
			if (extendedResponse.getStatusCode() == HttpStatus.SC_NOT_FOUND)
			{
				return null; // user with that name doesn't exists
			}
			if (extendedResponse.isSuccessful())
			{

				responseString = extendedResponse.getResponseString();

				new JSONObject(responseString).has("user");
				return new AccountInfo(BitbucketCommunicator.BITBUCKET);

			} else
			{
				log.debug("Server response was not successful! Http Status Code: " + extendedResponse.getStatusCode());
			}
		} catch (ResponseException e)
		{
			log.debug(e.getMessage());
		} catch (JSONException e)
		{
			log.debug("Error parsing json response: " + responseString + ". " + e.getMessage());
		}
		return null; // something went wrong, we don't have any account info.

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Repository> getRepositories(Organization organization)
	{
		try
		{
			String listReposUrl = "/users/" + organization.getName();
			final Authentication authentication = authenticationFactory.getAuthentication(organization);

			ExtendedResponseHandler.ExtendedResponse extendedResponse = requestHelper.getExtendedResponse(
					authentication, listReposUrl, null, getApiUrl(organization));
			if (extendedResponse.getStatusCode() == HttpStatus.SC_UNAUTHORIZED)
			{
				throw new SourceControlException.UnauthorisedException("Invalid credentials");
			}
			if (extendedResponse.isSuccessful())
			{
				String responseString = extendedResponse.getResponseString();
				return BitbucketRepositoriesParser.parseRepositoryNames(new JSONObject(responseString));
			} else
			{
				throw new ResponseException("Server response was not successful! Http Status Code: "
						+ extendedResponse.getStatusCode());
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

	private String getApiUrl(Organization organization)
	{
		return getApiUrl(organization.getHostUrl());
	}

	private String getApiUrl(Repository repository)
	{
		return getApiUrl(repository.getOrgHostUrl());
	}

	
    public static String getApiUrl(String hostUrl)
	{
		return hostUrl + "/!api/1.0";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Changeset getDetailChangeset(Repository repository, Changeset changeset)
	{
		try
		{
			String owner = repository.getOrgName();
			String slug = repository.getSlug();
			String node = changeset.getNode();

			Authentication auth = authenticationFactory.getAuthentication(repository);

			log.debug("Parse changeset [ {} ] [ {} ] [ {} ]", new String[] { owner, slug, node });
			final String urlPath = "/repositories/" + CustomStringUtils.encode(owner) + "/"
					+ CustomStringUtils.encode(slug) + "/changesets/" + CustomStringUtils.encode(node);
			String responseFilesString = requestHelper.get(auth, urlPath + "/diffstat?limit="
					+ Changeset.MAX_VISIBLE_FILES, null, getApiUrl(repository));
			return BitbucketChangesetFactory.getChangesetWithStatistics(changeset, responseFilesString);
		} catch (ResponseException e)
		{
			throw new SourceControlException("Could not get result", e);
		}

	}

	/**
	 * Gets the changesets.
	 * 
	 * @param repository
	 *            the repository
	 * @param startNode
	 *            the start node
	 * @param limit
	 *            the limit
	 * @param lastCommitDate
	 *            the last commit date
	 * @return the changesets
	 */
	public List<Changeset> getChangesets(final Repository repository, final String startNode, final int limit,
			final Date lastCommitDate)
	{
		return new Retryer<List<Changeset>>().retry(new Callable<List<Changeset>>()
		{
			@Override
			public List<Changeset> call()
			{
				return getChangesetsInternal(repository, startNode, limit, lastCommitDate);
			}
		});

	}

	private List<Changeset> getChangesetsInternal(final Repository repository, String startNode, int limit,
			Date lastCommitDate)
	{
		String owner = repository.getOrgName();
		String slug = repository.getSlug();

		Authentication auth = authenticationFactory.getAuthentication(repository);

		log.debug("Parse bitbucket changesets [ {} ] [ {} ] [ {} ] [ {} ]", new String[] { owner, slug, startNode,
				String.valueOf(limit) });
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("limit", String.valueOf(limit));
		if (startNode != null)
		{
			params.put("start", startNode);
		}

		List<Changeset> changesets = new ArrayList<Changeset>();

		try
		{
			ExtendedResponseHandler.ExtendedResponse extendedResponse = requestHelper.getExtendedResponse(auth,
					"/repositories/" + CustomStringUtils.encode(owner) + "/" + CustomStringUtils.encode(slug)
							+ "/changesets", params, getApiUrl(repository));

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

					final Changeset changeset = BitbucketChangesetFactory.parse(repository.getId(), json);
					if (lastCommitDate == null || lastCommitDate.before(changeset.getDate()))
					{
						changesets.add(changeset);
					}
				}
			} else
			{
				throw new ResponseException("Server response was not successful! Http Status Code: "
						+ extendedResponse.getStatusCode());
			}
		} catch (ResponseException e)
		{
			log.warn("Could not get changesets from node: {}", startNode);
			throw new SourceControlException("Error requesting changesets. Node: " + startNode + ". [" + e.getMessage()
					+ "]", e);
		} catch (JSONException e)
		{
			throw new SourceControlException("Could not parse json object", e);
		}
		return changesets;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterable<Changeset> getChangesets(final Repository repository, final Date lastCommitDate)
	{
		return new Iterable<Changeset>()
		{
			@Override
			public Iterator<Changeset> iterator()
			{
				return new BitbucketChangesetIterator(BitbucketCommunicator.this, repository, lastCommitDate);
			}
		};

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setupPostcommitHook(Repository repository, String postCommitUrl)
	{
		String owner = repository.getOrgName();
		String slug = repository.getSlug();
		Authentication auth = authenticationFactory.getAuthentication(repository);

		String urlPath = "/repositories/" + owner + "/" + slug + "/services";
		String postData = "type=post&URL=" + postCommitUrl;
		try
		{
			requestHelper.post(auth, urlPath, postData, getApiUrl(repository));
		} catch (ResponseException e)
		{
			throw new SourceControlException("Could not add postcommit hook", e);
		}
		bitbucketLinker.linkRepository(repository);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removePostcommitHook(Repository repository, String postCommitUrl)
	{
		bitbucketLinker.unlinkRepository(repository);

		String owner = repository.getOrgName();
		String slug = repository.getSlug();
		Authentication auth = authenticationFactory.getAuthentication(repository);

		String urlPath = "/repositories/" + owner + "/" + slug + "/services";
		// Find the hook
		try
		{
			String responseString = requestHelper.get(auth, urlPath, null, getApiUrl(repository));
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
					requestHelper.delete(auth, getApiUrl(repository), urlPath + "/" + id);
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getCommitUrl(Repository repository, Changeset changeset)
	{
		return MessageFormat.format("{0}/{1}/{2}/changeset/{3}?dvcsconnector={4}", repository.getOrgHostUrl(),
				repository.getOrgName(), repository.getSlug(), changeset.getNode(), pluginVersion);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFileCommitUrl(Repository repository, Changeset changeset, String file, int index)
	{
		return MessageFormat.format("{0}#chg-{1}", getCommitUrl(repository, changeset), file);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DvcsUser getUser(Repository repository, String username)
	{
		try
		{
			log.debug("Parse user [ {} ]", username);

			String responseString = requestHelper.get(Authentication.ANONYMOUS,
					"/users/" + CustomStringUtils.encode(username), null, getApiUrl(repository));
			return BitbucketUserFactory.parse(new JSONObject(responseString).getJSONObject("user"));
		} catch (ResponseException e)
		{
			log.debug("Could not load user [ " + username + " ]");
			return DvcsUser.UNKNOWN_USER;
		} catch (JSONException e)
		{
			log.debug("Could not load user [ " + username + " ]");
			return DvcsUser.UNKNOWN_USER;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getUserUrl(Repository repository, Changeset changeset)
	{
		return MessageFormat.format("{0}/{1}", repository.getOrgHostUrl(), changeset.getAuthor());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Group> getGroupsForOrganization(Organization organization)
	{
		List<Group> groups = new ArrayList<Group>();
		String urlPath = "/groups/" + organization.getName();
		Authentication auth = authenticationFactory.getAuthentication(organization);
		try
		{
			String responseString = requestHelper.get(auth, urlPath, null, getApiUrl(organization));
			JSONArray jsonArray = new JSONArray(responseString);
			for (int i = 0; i < jsonArray.length(); i++)
			{
				JSONObject jsonGroup = (JSONObject) jsonArray.get(i);
				groups.add(new Group(jsonGroup.getString("slug"), jsonGroup.getString("name")));
			}
		} catch (Exception e)
		{
			log.warn("Error getting groups for organization {} [{}]", organization.getName(), e.getMessage());
			throw new SourceControlException(e);
		}
		return groups;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean validateCredentials(Organization organization)
	{
		// try to obtain user's ssh keys to know if credentials are OK
		// @ http://confluence.atlassian.com/display/BITBUCKET/SSH+Keys
		String urlPath = "/ssh-keys/";
		// need to create it directly because here we have
		Authentication auth = new BasicAuthentication(organization.getCredential().getAdminUsername(), organization
				.getCredential().getAdminPassword());
		try
		{
			ExtendedResponse extendedResponse = requestHelper.getExtendedResponse(auth, urlPath,
					Collections.<String, Object> emptyMap(), getApiUrl(organization));
			int statusCode = extendedResponse.getStatusCode();
			if (statusCode == 401 || statusCode == 403)
			{
				return false;
			}
		} catch (Exception e)
		{
			log.warn("Error getting groups for organization {} [{}]", organization.getName(), e.getMessage());
			throw new SourceControlException(e);
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean supportsInvitation(Organization organization)
	{
		return organization.getDvcsType().equalsIgnoreCase(BITBUCKET);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void inviteUser(Organization organization, Collection<String> groupSlugs, String userEmail)
	{
		
		String apiUrl  = getApiUrl(organization);
		String baseEndpointUrl  = "/users/" + organization.getName() + "/invitations/" + userEmail + "/" + organization.getName() + "/";
		
		for (String group : groupSlugs)
		{
			
			try
			{ 
				log.debug("Going invite " + userEmail + " to group " + group + " of bitbucket organization " + organization.getName());
				
				ExtendedResponse response = requestHelper.runRequestGetExtendedResponse(MethodType.PUT,
															apiUrl, 
															baseEndpointUrl + group,
															authenticationFactory.getAuthentication(organization),
															null, null);
				
				if (!response.isSuccessful()) {
					log.warn("Failed to invite user {} to organization {}. Response HTTP code {}",
							new Object [] { userEmail, organization.getName(), response.getStatusCode() } );
				}
				
			} catch (ResponseException e)
			{ 
				log.warn("Failed to invite user {} to organization {}. Cause error message is {}",
						new Object [] { userEmail, organization.getName(), e.getMessage() } );
			}
			 
		}

	}

}
