package com.atlassian.jira.plugins.bitbucket.bitbucket.resource;

import com.atlassian.jira.util.json.JSONObject;

import java.util.Map;

/**
 */
public class SubRemoteResource implements RemoteResource
{
    private final RemoteResource parentResource;
    private final String uri;

    public SubRemoteResource(RemoteResource parentResource, String uri)
    {
        this.parentResource = parentResource;
        this.uri = uri;
    }

    public JSONObject get(String uri)
    {
        return parentResource.get(this.uri + "/" + uri);
    }

    public JSONObject get(String uri, Map<String,Object> params)
    {
        return parentResource.get(this.uri + "/" + uri, params);
    }
}
