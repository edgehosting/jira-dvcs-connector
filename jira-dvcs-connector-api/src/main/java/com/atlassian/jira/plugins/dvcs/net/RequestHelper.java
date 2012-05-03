package com.atlassian.jira.plugins.dvcs.net;

import com.atlassian.jira.plugins.dvcs.auth.Authentication;
import com.atlassian.sal.api.net.ResponseException;

import java.util.Map;

public interface RequestHelper
{
    public String get(Authentication auth, String urlPath, Map<String, Object> params, String apiBaseUrl) throws ResponseException;

    public ExtendedResponseHandler.ExtendedResponse getExtendedResponse(Authentication auth, String urlPath, Map<String, Object> params, String apiBaseUrl)
        throws ResponseException;

    public String post(Authentication auth, String urlPath, String postData, String apiBaseUrl) throws ResponseException;

    public void delete(Authentication auth, String apiUrl, String urlPath) throws ResponseException;

}