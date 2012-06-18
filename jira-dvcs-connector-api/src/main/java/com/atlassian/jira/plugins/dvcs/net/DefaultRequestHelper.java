package com.atlassian.jira.plugins.dvcs.net;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.auth.Authentication;
import com.atlassian.jira.plugins.dvcs.net.ExtendedResponseHandler.ExtendedResponse;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Request.MethodType;
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
    public ExtendedResponseHandler.ExtendedResponse getExtendedResponse(Authentication auth, String urlPath, Map<String, Object> params, String apiBaseUrl) throws ResponseException
    {
        
        ExtendedResponse extendedResponse = runRequestGetExtendedResponse(Request.MethodType.GET, apiBaseUrl, urlPath, auth, params, null);
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

    @SuppressWarnings({ "rawtypes" })
    private String runRequest(Request.MethodType methodType, String apiBaseUrl, String urlPath, Authentication auth,
                                Map<String, Object> params, String postData, ResponseHandler responseHandler) throws ResponseException
    {
       return runRequestGetExtendedResponse(methodType, apiBaseUrl, urlPath, auth, params, postData).getResponseString();
    }

    private ExtendedResponse runRequestGetExtendedResponse(Request.MethodType methodType, String apiBaseUrl, String urlPath, Authentication auth,
    		Map<String, Object> params, String postData) throws ResponseException
    		{
    	String url = apiBaseUrl + urlPath + buildQueryString(params);
    	log.debug(methodType + " [ " + url + " ]");
    	
    	HttpClient httpClient = new HttpClient();
    	HttpMethod method = getHttpMethodByType(methodType, url);
    	
    	
    	if (auth != null) {
    		auth.addAuthentication(method, httpClient);
    	}
    	
    	if (postData != null && methodType == MethodType.POST) {
    		Map<String, String> parameters = parseParameters(postData);
    		for (String paramName : parameters.keySet()) {
    			((PostMethod)method).addParameter(new NameValuePair(paramName, parameters.get(paramName)));
    		}
    	}
    	
    	method.getParams().setSoTimeout(6000);
    	
    	try {
    		
    		httpClient.executeMethod(method);
    		String response = method.getResponseBodyAsString();
    		
    		log.debug("returned: " + response);
    		
    		int statusCode = method.getStatusCode();
    		boolean successful = statusCode >= 200 && statusCode <= 300;
    		
    		if (!successful) {
    			throw new ResponseException("Request failed. Status Code = " + statusCode);
    		}
    		
    		return new ExtendedResponse(successful, statusCode, response);
    		
    	} catch (Exception e) {
    		log.warn("error execute method :  " + method, e);
    		throw new ResponseException("error execute method :  " + method, e);
    	}
    }
    
    private static Map<String, String> parseParameters(String postData) {
    	
    	Map<String, String> params = new java.util.HashMap<String, String>();
	    
    	if (StringUtils.isNotBlank(postData)) {
	    		
	    	String[] parameters = postData.split("&");
	    	for (String parameter : parameters) {
				String [] parameterTokens = parameter.split("=");
				params.put(parameterTokens[0], parameterTokens[1]);
			}
    	}
	    	
    	return params;
	}

	private org.apache.commons.httpclient.HttpMethod getHttpMethodByType(MethodType type, String uri) {
    	
    	switch (type) {
	    	case POST:
	    		return new PostMethod(uri);
	    	case DELETE:
	    		return new DeleteMethod(uri);
			default:
				return new GetMethod(uri);
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
