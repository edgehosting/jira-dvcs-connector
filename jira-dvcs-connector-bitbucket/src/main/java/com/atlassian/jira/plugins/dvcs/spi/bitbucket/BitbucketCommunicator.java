package com.atlassian.jira.plugins.dvcs.spi.bitbucket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.auth.Authentication;
import com.atlassian.jira.plugins.dvcs.auth.AuthenticationFactory;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.net.ExtendedResponseHandler;
import com.atlassian.jira.plugins.dvcs.net.RequestHelper;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.parsers.BitbucketChangesetFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.parsers.BitbucketRepositoriesParser;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.net.ResponseException;

public class BitbucketCommunicator implements DvcsCommunicator
{
    private static final Logger log = LoggerFactory.getLogger(BitbucketCommunicator.class);

    public static final String BITBUCKET = "bitbucket";

    private final RequestHelper requestHelper;
    private final AuthenticationFactory authenticationFactory;

    public BitbucketCommunicator(AuthenticationFactory authenticationFactory, RequestHelper requestHelper)
    {
        this.authenticationFactory = authenticationFactory;
        this.requestHelper = requestHelper;
    }

    @Override
    public String getDvcsType()
    {
        return BITBUCKET;
    }

    @Override
    public AccountInfo getAccountInfo(String hostUrl, String accountName)
    {
        // TODO: vracia to 200 reponse aj pre neexistujucu ORG!!!
        String responseString = null;
        try
        {
            String apiUrl = hostUrl + "/!api/1.0";
            String accountUrl = "/users/"+accountName;
            ExtendedResponseHandler.ExtendedResponse extendedResponse = requestHelper.getExtendedResponse(null, accountUrl, null, apiUrl);
            if (extendedResponse.getStatusCode() == HttpStatus.SC_NOT_FOUND)
            {
                return null; //user with that name doesn't exists
            }
            if (extendedResponse.isSuccessful())
            {
                responseString = extendedResponse.getResponseString();
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
        return null;    // something went wrong, we don't have any account info.

    }

    @Override
    public List<Repository> getRepositories(Organization organization)
    {
        try
        {
            String apiUrl = organization.getHostUrl() + "/!api/1.0";
            String listReposUrl = "/users/"+organization.getName();
            final Authentication authentication = authenticationFactory.getAuthentication(organization);

            ExtendedResponseHandler.ExtendedResponse extendedResponse = requestHelper.getExtendedResponse(authentication, listReposUrl, null, apiUrl);
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

            log.debug("Parse changeset [ {} ] [ {} ] [ {} ]", new String[]{owner, slug, node});
            final String urlPath = "/repositories/" + CustomStringUtils.encode(owner) + "/" +
                    CustomStringUtils.encode(slug) + "/changesets/" + CustomStringUtils.encode(node);
            String responseFilesString = requestHelper.get(auth, urlPath + "/diffstat?limit=" + Changeset.MAX_VISIBLE_FILES, null, apiUrl);
            return BitbucketChangesetFactory.getChangesetWithStatistics(changeset, responseFilesString);
        } catch (ResponseException e)
        {
            throw new SourceControlException("Could not get result", e);
        }

    }

    public List<Changeset> getChangesets(final Repository repository, String startNode, int limit, Date lastCommitDate)
    {
        String apiUrl = repository.getOrgHostUrl() + "/!api/1.0";
        String owner = repository.getOrgName();
        String slug = repository.getSlug();

        Authentication auth = authenticationFactory.getAuthentication(repository);

        log.debug("Parse bitbucket changesets [ {} ] [ {} ] [ {} ] [ {} ]", new String[]{owner, slug, startNode, String.valueOf(limit)});
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("limit", String.valueOf(limit));
        if (startNode != null)
        {
            params.put("start", startNode);
        }

        List<Changeset> changesets = new ArrayList<Changeset>();

        try
        {
            ExtendedResponseHandler.ExtendedResponse extendedResponse = requestHelper.getExtendedResponse(auth, "/repositories/" + CustomStringUtils.encode(owner)
                    + "/" + CustomStringUtils.encode(slug) + "/changesets", params, apiUrl);

            if (extendedResponse.getStatusCode() == HttpStatus.SC_UNAUTHORIZED)
            {
                throw new com.atlassian.jira.plugins.bitbucket.api.exception.SourceControlException("Incorrect credentials");
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
                throw new ResponseException("Server response was not successful! Http Status Code: " + extendedResponse.getStatusCode());
            }
        } catch (ResponseException e)
        {
            log.warn("Could not get changesets from node: {}", startNode);
            throw new SourceControlException("Error requesting changesets. Node: " + startNode + ". [" + e.getMessage() + "]", e);
        } catch (JSONException e)
        {
            throw new SourceControlException("Could not parse json object", e);
        }
        return changesets;
    }


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
        } catch (JSONException e)
        {
            log.warn("Error removing postcommit service [{}]", e.getMessage());
        }

    }
}
