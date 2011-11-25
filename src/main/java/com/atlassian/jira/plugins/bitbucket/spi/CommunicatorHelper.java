package com.atlassian.jira.plugins.bitbucket.spi;

import com.atlassian.jira.plugins.bitbucket.api.Authentication;
import com.atlassian.jira.plugins.bitbucket.api.impl.GithubOAuthAuthentication;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ResponseHandler;
import org.apache.commons.httpclient.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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
        Request<?, ?> request = requestFactory.createRequest(methodType, url);
        logger.debug("get [ " + url + " ]");
        if (auth instanceof GithubOAuthAuthentication)
        {
            String separator = (params == null || params.isEmpty()) ? "?" : "&";
            url+= separator + "access_token="+((GithubOAuthAuthentication) auth).getAccessToken();
        }
        if (auth != null) auth.addAuthentication(request, url);
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

    
    public static class ExtendedResponse
    {
        private final boolean successful;
        private final int statusCode;
        private final String responseString;

        public ExtendedResponse(boolean successful, int statusCode, String responseString)
        {
            this.successful = successful;
            this.statusCode = statusCode;
            this.responseString = responseString;
        }

        public boolean isSuccessful()
        {
            return successful;
        }

        public int getStatusCode()
        {
            return statusCode;
        }

        public String getResponseString()
        {
            return responseString;
        }
    }

    public static class ExtendedResponseHandler implements ResponseHandler<Response>
    {
        private final AtomicReference<ExtendedResponse> extendedResponse = new AtomicReference<ExtendedResponse>();
        @Override
        public void handle(Response response) throws ResponseException
        {
            ExtendedResponse er = new ExtendedResponse(response.isSuccessful(), response.getStatusCode(), response.getResponseBodyAsString());
            extendedResponse.set(er);
        }
        
        public ExtendedResponse getExtendedResponse()
        {
            return extendedResponse.get();
        }
    }
    
    public ExtendedResponse getRepositoryInfo(final RepositoryUri repositoryUri)
    {
        ExtendedResponseHandler responseHandler = new ExtendedResponseHandler();
        try
        {
            get(Authentication.ANONYMOUS, repositoryUri.getRepositoryInfoUrl(), null, repositoryUri.getApiUrl(), responseHandler);
        } catch (ResponseException e)
        {
            logger.warn(e.getMessage());
        }
        return responseHandler.getExtendedResponse();
    }
    
    public Boolean isRepositoryPrivate1(final RepositoryUri repositoryUri)
    {
        ExtendedResponse extendedResponse = getRepositoryInfo(repositoryUri);
        if (extendedResponse==null)
        {
            logger.warn("Unable to retrieve repository info for: " +repositoryUri.getRepositoryUrl());
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
                logger.debug(e.getMessage());
            } 
        }
        return null;
    }

    
}
