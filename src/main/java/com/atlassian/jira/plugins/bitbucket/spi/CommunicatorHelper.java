package com.atlassian.jira.plugins.bitbucket.spi;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.httpclient.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.bitbucket.api.Authentication;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ResponseHandler;

// TODO make it a component 
public class CommunicatorHelper
{
    private final Logger logger = LoggerFactory.getLogger(CommunicatorHelper.class);

    protected final RequestFactory<?> requestFactory;

    public CommunicatorHelper(RequestFactory<?> requestFactory)
    {
        this.requestFactory = requestFactory;
    }

    public String get(Authentication auth, String urlPath, Map<String, Object> params, String apiBaseUrl) throws ResponseException
    {
        return runRequest(Request.MethodType.GET, apiBaseUrl, urlPath, auth, params, null);
    }

    public void get(Authentication auth, String urlPath, Map<String, Object> params, String apiBaseUrl, ResponseHandler responseHandler) throws ResponseException
    {
        runRequest(Request.MethodType.GET, apiBaseUrl, urlPath, auth, params, null, responseHandler);
    }

    public String post(Authentication auth, String urlPath, String postData, String apiBaseUrl) throws ResponseException
    {
        return runRequest(Request.MethodType.POST, apiBaseUrl, urlPath, auth, null, postData);
    }

    public void delete(Authentication auth, String apiUrl, String urlPath) throws ResponseException
    {
        runRequest(Request.MethodType.DELETE, apiUrl, urlPath, auth, null, null);
    }

    private String runRequest(Request.MethodType methodType, String apiBaseUrl, String urlPath, Authentication auth,
        Map<String, Object> params, String postData) throws ResponseException
    {
        return runRequest(methodType, apiBaseUrl, urlPath, auth, params, postData, null);
    }

    private String runRequest(Request.MethodType methodType, String apiBaseUrl, String urlPath, Authentication auth,
                                Map<String, Object> params, String postData, ResponseHandler responseHandler) throws ResponseException
    {
        String url = apiBaseUrl + urlPath + buildQueryString(params);
        logger.debug("get [ " + url + " ]");
        Request<?, ?> request = requestFactory.createRequest(methodType, url);
        if (auth != null) auth.addAuthentication(request);
        if (postData != null) request.setRequestBody(postData);
        request.setSoTimeout(60000);
        if (responseHandler!=null)
        {
            request.execute(responseHandler);
            return null;
        } else
        {
            return request.execute();
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

    
    public static class RepositoryInfoResponseHandler implements ResponseHandler<Response>
    {
        private final Logger logger = LoggerFactory.getLogger(RepositoryInfoResponseHandler.class);

        private final AtomicReference<Boolean> isPrivate = new AtomicReference<Boolean>();
       
        @Override
        public void handle(Response response) throws ResponseException
        {
            if (isRepositoryAccessible(response))
            {
                isPrivate.set(false);
            } else if (isRepositoryPrivate(response))
            {
                isPrivate.set(true);
            } else
            {
                logger.debug("Response code " + response.getStatusCode() );
            }
        }

        private boolean isRepositoryPrivate(Response response)
        {
            return (response.getStatusCode() == HttpStatus.SC_UNAUTHORIZED);
        }

        private boolean isRepositoryAccessible(Response response) throws ResponseException
        {
            if (response.isSuccessful())
            {
                try
                {
                    String responseString = response.getResponseBodyAsString();
                    // is this valid JSON?
                    new JSONObject(responseString);
                    return true;
                } catch (JSONException e)
                {
                    logger.debug(e.getMessage());
                }
            }
            return false;
        }
        
        public Boolean isPrivate()
        {
            return isPrivate.get();
        }
    }
    
    public Boolean isRepositoryPrivate(final RepositoryUri repositoryUri)
    {
        try
        {
            RepositoryInfoResponseHandler responseHandler = new RepositoryInfoResponseHandler();
            get(Authentication.ANONYMOUS, repositoryUri.getRepositoryInfoUrl(), null, repositoryUri.getApiUrl(), responseHandler);
            return responseHandler.isPrivate();
        } catch (ResponseException e)
        {
            return null;
        }
    }

    
}
