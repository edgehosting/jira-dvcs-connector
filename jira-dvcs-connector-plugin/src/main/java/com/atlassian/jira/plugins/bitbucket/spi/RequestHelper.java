package com.atlassian.jira.plugins.bitbucket.spi;

import java.util.Map;

import com.atlassian.jira.plugins.bitbucket.api.Authentication;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.spi.ExtendedResponseHandler.ExtendedResponse;
import com.atlassian.sal.api.net.ResponseException;

public interface RequestHelper
{

    public String get(Authentication auth, String urlPath, Map<String, Object> params, String apiBaseUrl) throws ResponseException;

    public ExtendedResponse getExtendedResponse(Authentication auth, String urlPath, Map<String, Object> params, String apiBaseUrl)
        throws ResponseException;

    public String post(Authentication auth, String urlPath, String postData, String apiBaseUrl) throws ResponseException;

    public void delete(Authentication auth, String apiUrl, String urlPath) throws ResponseException;

    public Boolean isRepositoryPrivate1(final RepositoryUri repositoryUri);

}