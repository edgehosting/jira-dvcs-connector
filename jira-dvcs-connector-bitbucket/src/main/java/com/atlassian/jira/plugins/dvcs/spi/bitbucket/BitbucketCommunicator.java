package com.atlassian.jira.plugins.dvcs.spi.bitbucket;

import com.atlassian.jira.plugins.dvcs.auth.Authentication;
import com.atlassian.jira.plugins.dvcs.auth.AuthenticationFactory;
import com.atlassian.jira.plugins.dvcs.auth.impl.BasicAuthentication;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.net.ExtendedResponseHandler;
import com.atlassian.jira.plugins.dvcs.net.RequestHelper;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.parsers.BitbucketRepositoriesParser;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.net.ResponseException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BitbucketCommunicator implements DvcsCommunicator
{
    private static final Logger log = LoggerFactory.getLogger(BitbucketCommunicator.class);

    public static final String BITBUCKET = "bitbucket";

    private final RequestHelper requestHelper;
    private AuthenticationFactory authenticationFactory;

    public BitbucketCommunicator(AuthenticationFactory authenticationFactory, RequestHelper requestHelper)
    {
        this.authenticationFactory = authenticationFactory;
        this.requestHelper = requestHelper;
    }

    private Authentication getAuthentication(String adminUsername, String adminPassword)
    {
        Authentication auth;
        if (StringUtils.isNotBlank(adminUsername) && StringUtils.isNotBlank(adminPassword))
        {
            auth = new BasicAuthentication(adminUsername, adminPassword);
        } else
        {
            auth = Authentication.ANONYMOUS;
        }
        return auth;
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
            final Authentication authentication = getAuthentication(organization.getCredential().getAdminUsername(), organization.getCredential().getAdminPassword());
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
}
