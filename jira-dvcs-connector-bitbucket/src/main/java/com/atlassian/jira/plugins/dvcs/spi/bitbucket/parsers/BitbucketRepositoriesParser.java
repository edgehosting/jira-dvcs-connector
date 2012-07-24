//package com.atlassian.jira.plugins.dvcs.spi.bitbucket.parsers;
//
//import com.atlassian.jira.plugins.dvcs.model.Repository;
//import com.atlassian.jira.util.json.JSONArray;
//import com.atlassian.jira.util.json.JSONException;
//import com.atlassian.jira.util.json.JSONObject;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class BitbucketRepositoriesParser
//{
//
//    public static List<Repository> parseRepositoryNames(JSONObject jsonObject) throws JSONException
//    {
//        List<Repository> repositories = new ArrayList<Repository>();
//        JSONArray jsonArray = jsonObject.getJSONArray("repositories");
//        for (int i = 0; i < jsonArray.length(); i++)
//        {
//            JSONObject repositoryJson  = (JSONObject) jsonArray.get(i);
//
//            Repository repository = new Repository();
//            repository.setSlug(repositoryJson.getString("slug"));
//            repository.setName(repositoryJson.getString("name"));
//            repositories.add(repository);
//        }
//        return repositories;
//    }
//
//}
