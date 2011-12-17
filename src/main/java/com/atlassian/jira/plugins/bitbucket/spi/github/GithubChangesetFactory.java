package com.atlassian.jira.plugins.bitbucket.spi.github;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.ChangesetFile;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.spi.CustomStringUtils;
import com.atlassian.jira.plugins.bitbucket.spi.DefaultBitbucketChangeset;
import com.atlassian.jira.plugins.bitbucket.spi.DefaultBitbucketChangesetFile;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

import javax.xml.bind.DatatypeConverter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Factory for {@link com.atlassian.jira.plugins.bitbucket.api.Changeset} implementations
 */
public class GithubChangesetFactory
{

    /**
     * Parse the json object as a bitbucket changeset
     *
     * @param json  the json object describing the change
     * @return the parsed {@link com.atlassian.jira.plugins.bitbucket.api.Changeset}
     */
    public static Changeset parse(int repositoryId, String branch, JSONObject json)
    {
        try
        {
            JSONObject commitJson = json.getJSONObject("commit");
            JSONObject commitAuthor = commitJson.getJSONObject("author");

            // TODO this can be null see https://sdog.jira.com/browse/BBC-62
            JSONObject author = json.getJSONObject("author");

            List<ChangesetFile> changesetFiles = fileList(json.getJSONArray("files"), false);

            return new DefaultBitbucketChangeset(
                    repositoryId,
                    json.getString("sha"),
                    commitAuthor.has("name") ? commitAuthor.getString("name") : "",
                    author.has("login") ? author.getString("login") : "",
                    parseDate(commitAuthor.getString("date")),
                    "", // todo: raw-node. what is it in github?
                    branch,
                    commitJson.getString("message"),
                    parentList(json.getJSONArray("parents")),
                    changesetFiles,
                    changesetFiles.size()
            );
        }

        catch (JSONException e)
        {
            throw new SourceControlException("invalid json object", e);
        }
    }

    public static Date parseDate(String dateStr) {
        // Atom (ISO 8601) example: 2011-11-09T06:24:13-08:00

        try {
            Calendar calendar = DatatypeConverter.parseDateTime(dateStr);
            return calendar.getTime();
        } catch (IllegalArgumentException e) {
            throw new SourceControlException("Could not parse date string from JSON.", e);
        }

    }


    private static List<String> parentList(JSONArray parents) throws JSONException
    {
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < parents.length(); i++)
            list.add(((JSONObject) parents.get(i)).getString("sha"));
        return list;
    }

    private static List<ChangesetFile> fileList(JSONArray files, boolean fromPostcommitHook) throws JSONException
    {
        List<ChangesetFile> list = new ArrayList<ChangesetFile>();

        for (int i = 0; i < files.length(); i++)
        {
            JSONObject file = files.getJSONObject(i);
            String filename = file.getString("filename");
            String status = file.getString("status");
            int additions = file.getInt("additions");
            int deletions = file.getInt("deletions");

            list.add(new DefaultBitbucketChangesetFile(CustomStringUtils.getChangesetFileAction(status),
                    filename, additions, deletions));
        }

        return list;
    }


    private GithubChangesetFactory()
    {
    }
}
