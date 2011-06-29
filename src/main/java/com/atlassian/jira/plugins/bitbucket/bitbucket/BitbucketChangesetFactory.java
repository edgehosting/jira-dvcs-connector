package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.plugins.bitbucket.bitbucket.impl.DefaultBitbucketChangeset;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for {@link BitbucketChangeset} implementations
 */
public class BitbucketChangesetFactory
{
    /**
     * Parse the json object as a bitbucket changeset
     * @param owner the owner of the repository this changeset belongs to
     * @param slug the slug of the repository this changeset belons to
     * @param json the json object describing the change
     * @return the parsed {@link BitbucketChangeset}
     */
    public static BitbucketChangeset parse(String owner, String slug, JSONObject json)
    {
        try
        {
            return new DefaultBitbucketChangeset(
                    owner, slug,
                    json.getString("node"),
                    json.getString("raw_author"),
                    json.getString("author"),
                    json.getString("timestamp"),
                    json.getString("raw_node"),
                    json.getString("branch"),
                    json.getString("message"),
                    json.getInt("revision"),
                    stringList(json.getJSONArray("parents")),
                    fileList(json.getJSONArray("files"))
            );
        }
        catch (JSONException e)
        {
            throw new BitbucketException("invalid json object", e);
        }
    }

    private static List<String> stringList(JSONArray parents) throws JSONException
    {
        List<String> list = new ArrayList<String>();
        for(int i=0;i<parents.length();i++)
            list.add((String) parents.get(i));
        return list;
    }

    private static List<BitbucketChangesetFile> fileList(JSONArray parents) throws JSONException
    {
        List<BitbucketChangesetFile> list = new ArrayList<BitbucketChangesetFile>();
        for(int i=0;i<parents.length();i++)
            list.add(BitbucketChangesetFileFactory.parse((JSONObject) parents.get(i)));
        return list;
    }

    private BitbucketChangesetFactory()
    {
    }
}
