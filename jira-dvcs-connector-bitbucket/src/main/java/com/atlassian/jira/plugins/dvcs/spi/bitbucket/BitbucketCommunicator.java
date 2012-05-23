package com.atlassian.jira.plugins.dvcs.spi.bitbucket;

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
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.parsers.BitbucketChangesetFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.parsers.BitbucketRepositoriesParser;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.parsers.BitbucketUserFactory;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.net.ResponseException;
import org.apache.commons.httpclient.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The Class BitbucketCommunicator.
 */
public class BitbucketCommunicator implements DvcsCommunicator
{
	
	/** The Constant log. */
	private static final Logger log = LoggerFactory.getLogger(BitbucketCommunicator.class);

	/** The Constant BITBUCKET. */
	public static final String BITBUCKET = "bitbucket";

	/** The request helper. */
	private final RequestHelper requestHelper;
	
	/** The authentication factory. */
	private final AuthenticationFactory authenticationFactory;

	/**
	 * The Constructor.
	 *
	 * @param authenticationFactory the authentication factory
	 * @param requestHelper the request helper
	 */
	public BitbucketCommunicator(AuthenticationFactory authenticationFactory, RequestHelper requestHelper)
	{
		this.authenticationFactory = authenticationFactory;
		this.requestHelper = requestHelper;
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
		// TODO: it returns 200 even for non-existing ORG!!!
		String responseString = null;
		try
		{
			String apiUrl = hostUrl + "/!api/1.0";
			String accountUrl = "/users/" + accountName;
			ExtendedResponseHandler.ExtendedResponse extendedResponse = requestHelper.getExtendedResponse(null,
					accountUrl, null, apiUrl);
			if (extendedResponse.getStatusCode() == HttpStatus.SC_NOT_FOUND)
			{
				return null; // user with that name doesn't exists
			}
			if (extendedResponse.isSuccessful())
			{

				responseString = extendedResponse.getResponseString();
				// TODO not used, delete if no need
				final boolean isUserJson = new JSONObject(responseString).has("user");
				return new AccountInfo(BitbucketCommunicator.BITBUCKET);

			} else
			{
				log.error("Server response was not successful! Http Status Code: " + extendedResponse.getStatusCode());
			}
		} catch (ResponseException e)
		{
			log.error(e.getMessage(), e);
		} catch (JSONException e)
		{
			log.error("Error parsing json response: " + responseString, e);
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
			String apiUrl = organization.getHostUrl() + "/!api/1.0";
			String listReposUrl = "/users/" + organization.getName();
			final Authentication authentication = authenticationFactory.getAuthentication(organization);

			ExtendedResponseHandler.ExtendedResponse extendedResponse = requestHelper.getExtendedResponse(
					authentication, listReposUrl, null, apiUrl);
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Changeset getDetailChangeset(Repository repository, Changeset changeset)
	{
		try
		{
			String apiUrl = repository.getOrgHostUrl() + "/!api/1.0";
			String owner = repository.getOrgName();
			String slug = repository.getSlug();
			String node = changeset.getNode();

			Authentication auth = authenticationFactory.getAuthentication(repository);

			log.debug("Parse changeset [ {} ] [ {} ] [ {} ]", new String[] { owner, slug, node });
			final String urlPath = "/repositories/" + CustomStringUtils.encode(owner) + "/"
					+ CustomStringUtils.encode(slug) + "/changesets/" + CustomStringUtils.encode(node);
			String responseFilesString = requestHelper.get(auth, urlPath + "/diffstat?limit="
					+ Changeset.MAX_VISIBLE_FILES, null, apiUrl);
			return BitbucketChangesetFactory.getChangesetWithStatistics(changeset, responseFilesString);
		} catch (ResponseException e)
		{
			throw new SourceControlException("Could not get result", e);
		}

	}

	/**
	 * Gets the changesets.
	 *
	 * @param repository the repository
	 * @param startNode the start node
	 * @param limit the limit
	 * @param lastCommitDate the last commit date
	 * @return the changesets
	 */
	public List<Changeset> getChangesets(final Repository repository, String startNode, int limit, Date lastCommitDate)
	{
		String apiUrl = repository.getOrgHostUrl() + "/!api/1.0";
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
							+ "/changesets", params, apiUrl);

			if (extendedResponse.getStatusCode() == HttpStatus.SC_UNAUTHORIZED)
			{
				throw new com.atlassian.jira.plugins.bitbucket.api.exception.SourceControlException(
						"Incorrect credentials");
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
		String apiUrl = repository.getOrgHostUrl() + "/!api/1.0";
		String owner = repository.getOrgName();
		String slug = repository.getSlug();
		Authentication auth = authenticationFactory.getAuthentication(repository);

		String urlPath = "/repositories/" + owner + "/" + slug + "/services";
		String postData = "type=post;URL=" + postCommitUrl;
		try
		{
			requestHelper.post(auth, urlPath, postData, apiUrl);
		} catch (ResponseException e)
		{
			throw new SourceControlException("Could not add postcommit hook", e);
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removePostcommitHook(Repository repository, String postCommitUrl)
	{
		String apiUrl = repository.getOrgHostUrl() + "/!api/1.0";
		String owner = repository.getOrgName();
		String slug = repository.getSlug();
		Authentication auth = authenticationFactory.getAuthentication(repository);

		String urlPath = "/repositories/" + owner + "/" + slug + "/services";
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
		return MessageFormat.format("{0}/{1}/{2}/changeset/{3}", repository.getOrgHostUrl(), repository.getOrgName(),
				repository.getSlug(), changeset.getNode());
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
			String apiUrl = repository.getOrgHostUrl() + "/!api/1.0";
			log.debug("Parse user [ {} ]", username);

			String responseString = requestHelper.get(Authentication.ANONYMOUS,
					"/users/" + CustomStringUtils.encode(username), null, apiUrl);
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
		String apiUrl = organization.getHostUrl() + "/!api/1.0";
		String urlPath = "/groups/" + organization.getName();
		Authentication auth = authenticationFactory.getAuthentication(organization);
		try
		{
			String responseString = requestHelper.get(auth, urlPath, null, apiUrl);
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
		String apiUrl = organization.getHostUrl() + "/!api/1.0";
		// try to obtain user's ssh keys to know if credentials are OK
		// @ http://confluence.atlassian.com/display/BITBUCKET/SSH+Keys
		String urlPath = "/ssh-keys/";	
		// need to create it directly because here we have
		Authentication auth = new BasicAuthentication(organization.getCredential().getAdminUsername(), organization.getCredential().getAdminPassword());
		try
		{
			ExtendedResponse extendedResponse = requestHelper.getExtendedResponse(auth, urlPath, Collections.EMPTY_MAP, apiUrl);
			int statusCode = extendedResponse.getStatusCode();
			if (statusCode == 401 || statusCode == 403) {
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
		// TODO
	}

}
