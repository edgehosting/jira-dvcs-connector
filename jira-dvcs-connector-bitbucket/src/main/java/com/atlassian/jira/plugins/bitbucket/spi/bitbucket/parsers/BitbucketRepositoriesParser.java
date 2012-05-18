package com.atlassian.jira.plugins.bitbucket.spi.bitbucket.parsers;

import java.util.ArrayList;
import java.util.List;

import com.atlassian.jira.plugins.bitbucket.api.rest.AccountInfo;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketRepositoryManager;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

public class BitbucketRepositoriesParser
{

    public static List<String> parseRepositoryNames(JSONObject jsonObject) throws JSONException
    {
        List<String> repositoryNames = new ArrayList<String>();
        JSONArray jsonArray = jsonObject.getJSONArray("repositories");
        for (int i = 0; i < jsonArray.length(); i++)
        {
            JSONObject repository = (JSONObject) jsonArray.get(i);
            repositoryNames.add(repository.getString("slug"));
        }
        return repositoryNames;
    }

    /*
    "user": {
        "username": "dusanhornik",
        "first_name": "",
        "last_name": "",
        "avatar": "https://secure.gravatar.com/avatar/e8209d16f7811a9919f80cc141f4e2eb?d=identicon&s=32",
        "resource_uri": "/1.0/users/dusanhornik"
    }
    */
    public static AccountInfo parseAccount(String server, JSONObject jsonObject) throws JSONException
    {
        JSONObject jsonUser = jsonObject.getJSONObject("user");
        return new AccountInfo(server, jsonUser.getString("username"), jsonUser.getString("avatar"), BitbucketRepositoryManager.BITBUCKET);
    }
}
