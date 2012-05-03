package com.atlassian.jira.plugins.dvcs.spi.bitbucket.parsers;

import com.atlassian.jira.plugins.bitbucket.api.ChangesetFile;
import com.atlassian.jira.plugins.bitbucket.api.ChangesetFileAction;
import com.atlassian.jira.plugins.bitbucket.api.exception.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.impl.DefaultBitbucketChangesetFile;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

public class BitbucketChangesetFileFactory
{
    /**
     * Parse the json object as a {@link com.atlassian.jira.plugins.bitbucket.api.ChangesetFile file} within a changeset.
     *
     * @param json the json object describing the file
     * @return the parsed {@link com.atlassian.jira.plugins.bitbucket.api.ChangesetFile}
     */
    public static ChangesetFile parseFromDiffstatJson(JSONObject json)
    {
        try
        {
            JSONObject diffstatJson = json.getJSONObject("diffstat");
            return new DefaultBitbucketChangesetFile(
                    ChangesetFileAction.valueOf(json.getString("type").toUpperCase()),
                    json.getString("file"), diffstatJson.getInt("added"), diffstatJson.getInt("removed"));
        } catch (JSONException e)
        {
            throw new SourceControlException("invalid json object", e);
        }
    }

    /**
     * Parse the json object as a {@link com.atlassian.jira.plugins.bitbucket.api.ChangesetFile file} within a changeset.
     *
     * @param json the json object describing the file
     * @return the parsed {@link com.atlassian.jira.plugins.bitbucket.api.ChangesetFile}
     */
    public static ChangesetFile parseFromBaseJson(JSONObject json)
    {
        try
        {
            return new DefaultBitbucketChangesetFile(
                    ChangesetFileAction.valueOf(json.getString("type").toUpperCase()),
                    json.getString("file"), 0, 0);
        } catch (JSONException e)
        {
            throw new SourceControlException("invalid json object", e);
        }
    }

    private BitbucketChangesetFileFactory()
    {
    }
}
