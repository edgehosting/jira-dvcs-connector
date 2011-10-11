package com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl;

import com.atlassian.jira.plugins.bitbucket.api.impl.DefaultSourceControlUser;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketException;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

public class BitbucketUserFactory
{

	public static DefaultSourceControlUser parse(JSONObject json)
	{
	    try
	    {
	        return new DefaultSourceControlUser(
	                json.getJSONObject("user").getString("username"),
	                json.getJSONObject("user").getString("first_name"),
	                json.getJSONObject("user").getString("last_name"),
	                json.getJSONObject("user").getString("avatar"),
	                json.getJSONObject("user").getString("resource_uri")
	        );
	    }
	    catch (JSONException e)
	    {
	        throw new BitbucketException("invalid json object", e);
	    }
	}

}
