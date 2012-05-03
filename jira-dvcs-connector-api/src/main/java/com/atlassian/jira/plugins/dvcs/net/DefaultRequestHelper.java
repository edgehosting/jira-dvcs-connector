package com.atlassian.jira.plugins.dvcs.net;

import com.atlassian.jira.plugins.dvcs.auth.Authentication;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

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
    public ExtendedResponseHandler.ExtendedResponse getExtendedResponse(Authentication auth, String urlPath, Map<String, Object> params, String apiBaseUrl) throws ResponseException
    {
        ExtendedResponseHandler responseHandler = responseHandlerFactory.create();
        runRequest(Request.MethodType.GET, apiBaseUrl, urlPath, auth, params, null, responseHandler);
        ExtendedResponseHandler.ExtendedResponse extendedResponse = responseHandler.getExtendedResponse();

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



}
