package com.atlassian.jira.plugins.bitbucket.spi.github;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.ChangesetFile;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.impl.DefaultBitbucketChangesetFile;
import com.atlassian.jira.plugins.bitbucket.api.impl.DefaultChangeset;
import com.atlassian.jira.plugins.bitbucket.api.util.CustomStringUtils;
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
     * Parse the json object from GitHub v2. API as a changeset. We need only minimal information from that.
     *
     * @param json  the json object describing the change
     * @return the parsed {@link com.atlassian.jira.plugins.bitbucket.api.Changeset} with minimal fields
     */
    public static Changeset parseV2(int repositoryId, JSONObject commitJson) throws JSONException
    {

        String id = commitJson.getString("id");
        String msg = commitJson.getString("message");
        Date date = parseDate(commitJson.getString("committed_date"));

        return new DefaultChangeset(repositoryId, id, msg, date);
    }


    /**
     * Parse the json object from GitHub v3. API as a changeset
     *
     * @param json  the json object describing the change
     * @return the parsed {@link com.atlassian.jira.plugins.bitbucket.api.Changeset}
     */
    public static Changeset parseV3(int repositoryId, String branch, JSONObject json)
    {
        try
        {

            JSONObject commitJson = json.getJSONObject("commit");

            String name = "";
            Date date = Calendar.getInstance().getTime();
            if (commitJson.has("author") && !commitJson.isNull("author"))
            {
                JSONObject commitAuthor = commitJson.getJSONObject("author");
                name = commitAuthor.has("name") ? commitAuthor.getString("name") : "";
                date = parseDate(commitAuthor.getString("date"));
            }


            String login = "";
            if (json.has("author") && !json.isNull("author"))
            {
                JSONObject author = json.getJSONObject("author");
                login = author.has("login") ? author.getString("login") : "";
            }

            List<ChangesetFile> changesetFiles = fileList(json.getJSONArray("files"));

            return new DefaultChangeset(
                    repositoryId,
                    json.getString("sha"),
                    name,
                    login,
                    date,
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

    private static List<ChangesetFile> fileList(JSONArray files) throws JSONException
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
