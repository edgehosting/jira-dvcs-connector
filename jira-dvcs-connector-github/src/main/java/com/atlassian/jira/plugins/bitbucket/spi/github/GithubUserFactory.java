package com.atlassian.jira.plugins.bitbucket.spi.github;

import com.atlassian.jira.plugins.bitbucket.api.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

public class GithubUserFactory
{

    public static DvcsUser parse(JSONObject userJson)
    {
        try
        {
            String gravatarHash = userJson.getString("gravatar_id");
            String gravatarUrl = "https://secure.gravatar.com/avatar/" + gravatarHash + "?s=60";


            String login = userJson.getString("login");
            return new DvcsUser(
                    login,
                    "",
                    userJson.has("name") ? userJson.getString("name") : login,  // first and last name is together in github
                    gravatarUrl);
        }
        catch (JSONException e)
        {
            throw new SourceControlException("invalid json object", e);
        }
    }

}
