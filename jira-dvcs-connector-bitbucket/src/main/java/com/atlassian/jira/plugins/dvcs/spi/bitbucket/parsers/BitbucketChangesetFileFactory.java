package com.atlassian.jira.plugins.dvcs.spi.bitbucket.parsers;

import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileAction;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

public class BitbucketChangesetFileFactory
{
    /**
     * Parse the json object as a {@link ChangesetFile file} within a changeset.
     *
     * @param json the json object describing the file
     * @return the parsed {@link ChangesetFile}
     */
    public static ChangesetFile parseFromDiffstatJson(JSONObject json)
    {
        try
        {
            JSONObject diffstatJson = json.getJSONObject("diffstat");
            return new ChangesetFile(
                    ChangesetFileAction.valueOf(json.getString("type").toUpperCase()),
                    json.getString("file"), diffstatJson.getInt("added"), diffstatJson.getInt("removed"));
        } catch (JSONException e)
        {
            throw new SourceControlException("invalid json object", e);
        }
    }

    /**
     * Parse the json object as a {@link ChangesetFile file} within a changeset.
     *
     * @param json the json object describing the file
     * @return the parsed {@link ChangesetFile}
     */
    public static ChangesetFile parseFromBaseJson(JSONObject json)
    {
        try
        {
            return new ChangesetFile(
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
