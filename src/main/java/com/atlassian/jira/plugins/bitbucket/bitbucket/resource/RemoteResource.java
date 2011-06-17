package com.atlassian.jira.plugins.bitbucket.bitbucket.resource;

import com.atlassian.jira.util.json.JSONObject;

import java.util.Map;

/**
 */
public interface RemoteResource
{
    JSONObject get(String uri);

    JSONObject get(String uri, Map<String,Object> params);
}
