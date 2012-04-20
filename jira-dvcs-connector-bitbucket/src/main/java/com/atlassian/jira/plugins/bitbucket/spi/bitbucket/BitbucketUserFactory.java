package com.atlassian.jira.plugins.bitbucket.spi.bitbucket;

import com.atlassian.jira.plugins.bitbucket.api.exception.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.impl.DefaultSourceControlUser;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

public class BitbucketUserFactory
{

	public static DefaultSourceControlUser parse(JSONObject userJson)
	{
	    try
	    {
	        return new DefaultSourceControlUser(
	                userJson.getString("username"),
	                userJson.getString("first_name"),
	                userJson.getString("last_name"),
	                userJson.getString("avatar"),
	                userJson.getString("resource_uri")
	        );
	    }
	    catch (JSONException e)
	    {
	        throw new SourceControlException("invalid json object", e);
	    }
	}

}
