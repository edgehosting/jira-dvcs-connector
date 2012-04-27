package com.atlassian.jira.plugins.bitbucket.spi.bitbucket.parsers;

import java.util.ArrayList;
import java.util.List;

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
}
