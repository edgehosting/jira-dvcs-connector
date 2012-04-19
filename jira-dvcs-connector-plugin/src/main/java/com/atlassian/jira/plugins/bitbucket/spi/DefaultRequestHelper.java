package com.atlassian.jira.plugins.bitbucket.spi;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.httpclient.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.bitbucket.api.Authentication;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.api.RequestHelper;
import com.atlassian.jira.plugins.bitbucket.api.impl.ExtendedResponseHandler;
import com.atlassian.jira.plugins.bitbucket.api.impl.ExtendedResponseHandler.ExtendedResponse;
import com.atlassian.jira.plugins.bitbucket.api.util.CustomStringUtils;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ResponseHandler;

public class DefaultRequestHelper implements RequestHelper 
{

    private final Logger log = LoggerFactory.getLogger(DefaultRequestHelper.class);
    private final RequestFactory<?> requestFactory;
    private final ExtendedResponseHandlerFactory responseHandlerFactory;

    /**
     * For testing only
     */
    public DefaultRequestHelper(RequestFactory<?> requestFactory, ExtendedResponseHandlerFactory responseHandlerFactory)
    {
        this.requestFactory = requestFactory;
        this.responseHandlerFactory = responseHandlerFactory;
    }

    public DefaultRequestHelper(RequestFactory<?> requestFactory)
    {
        this.requestFactory = requestFactory;
        this.responseHandlerFactory = new DefaultExtendedResponseHandlerFactory();
    }

    @Override
    public String get(Authentication auth, String urlPath, Map<String, Object> params, String apiBaseUrl) throws ResponseException
    {
        return runRequest(Request.MethodType.GET, apiBaseUrl, urlPath, auth, params, null);
    }

    @Override
    public ExtendedResponse getExtendedResponse(Authentication auth, String urlPath, Map<String, Object> params, String apiBaseUrl) throws ResponseException
    {
        ExtendedResponseHandler responseHandler = responseHandlerFactory.create();
        runRequest(Request.MethodType.GET, apiBaseUrl, urlPath, auth, params, null, responseHandler);
        ExtendedResponse extendedResponse = responseHandler.getExtendedResponse();

        log.debug("returned: " + extendedResponse);

        return extendedResponse;
    }

    @Override
    public String post(Authentication auth, String urlPath, String postData, String apiBaseUrl) throws ResponseException
    {
        return runRequest(Request.MethodType.POST, apiBaseUrl, urlPath, auth, null, postData);
    }

    @Override
    public void delete(Authentication auth, String apiUrl, String urlPath) throws ResponseException
    {
        runRequest(Request.MethodType.DELETE, apiUrl, urlPath, auth, null, null);
    }

    private String runRequest(Request.MethodType methodType, String apiBaseUrl, String urlPath, Authentication auth,
        Map<String, Object> params, String postData) throws ResponseException
    {
        return runRequest(methodType, apiBaseUrl, urlPath, auth, params, postData, null);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private String runRequest(Request.MethodType methodType, String apiBaseUrl, String urlPath, Authentication auth,
                                Map<String, Object> params, String postData, ResponseHandler responseHandler) throws ResponseException
    {
        String url = apiBaseUrl + urlPath + buildQueryString(params);
        log.debug("get [ " + url + " ]");
        Request<?, ?> request = requestFactory.createRequest(methodType, url);
             
        if (auth != null) auth.addAuthentication(request, url);
        if (postData != null) request.setRequestBody(postData);
        request.setSoTimeout(60000);
        if (responseHandler!=null)
        {
            request.execute(responseHandler);
            return null;
        } else
        {
            String response = request.execute();
            log.debug("returned: " + response);
            return response;
        }
    }

    private String buildQueryString(Map<String, Object> params)
    {
        StringBuilder queryStringBuilder = new StringBuilder();

        if (params != null && !params.isEmpty())
        {
            queryStringBuilder.append("?");
            for (Iterator<Map.Entry<String, Object>> iterator = params.entrySet().iterator(); iterator.hasNext();)
            {
                Map.Entry<String, Object> entry = iterator.next();
                queryStringBuilder.append(CustomStringUtils.encode(entry.getKey()));
                queryStringBuilder.append("=");
                queryStringBuilder.append(CustomStringUtils.encode(String.valueOf(entry.getValue())));
                if (iterator.hasNext()) queryStringBuilder.append("&");
            }
        }
        return queryStringBuilder.toString();
    }


    @Override
    public Boolean isRepositoryPrivate1(final RepositoryUri repositoryUri)
    {
        ExtendedResponse extendedResponse = null;
        try
        {
            extendedResponse = getExtendedResponse(Authentication.ANONYMOUS, repositoryUri.getRepositoryInfoUrl(), null, repositoryUri.getApiUrl());
        } catch (ResponseException e)
        {
            log.warn(e.getMessage());
        }
        
        if (extendedResponse==null)
        {
            log.warn("Unable to retrieve repository info for: " +repositoryUri.getRepositoryUrl());
            return null;
        }
        
        if (extendedResponse.getStatusCode() == HttpStatus.SC_UNAUTHORIZED)
        {
            // this looks like a private repository
            return true;
        }
        
        if (extendedResponse.isSuccessful())
        {
            try
            {
                // this looks like public repo. Lets check if response looks parseable
                new JSONObject(extendedResponse.getResponseString());
                // everything looks fine, this repository is not public
                return false;
            } catch (JSONException e)
            {
                log.debug(e.getMessage());
            } 
        }
        return null;
    }

    
}
