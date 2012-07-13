package com.atlassian.jira.plugins.dvcs.spi.bitbucket.parsers;

import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

public class BitbucketUserFactory
{

	public static DvcsUser parse(JSONObject userJson)
	{
	    try
	    {
	        return new DvcsUser(
	                userJson.getString("username"),
	                userJson.getString("first_name") + " " + userJson.getString("last_name"),
	                userJson.getString("avatar")
	        );
	    }
	    catch (JSONException e)
	    {
	        throw new SourceControlException("invalid json object", e);
	    }
	}

}
