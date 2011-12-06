package com.atlassian.jira.plugins.bitbucket.spi.github;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.ChangesetFile;
import com.atlassian.jira.plugins.bitbucket.api.ChangesetFileAction;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.Communicator;
import com.atlassian.jira.plugins.bitbucket.spi.DefaultBitbucketChangeset;
import com.atlassian.jira.plugins.bitbucket.spi.DefaultBitbucketChangesetFile;
import com.atlassian.jira.plugins.bitbucket.spi.LazyLoadedBitbucketChangeset;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

import javax.xml.bind.DatatypeConverter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Factory for {@link com.atlassian.jira.plugins.bitbucket.api.Changeset} implementations
 */
public class GithubChangesetFactory
{
    /**
     * Load the changeset details based on the authentication method, the repository owner, repository
     * slug, and changeset node id
     *
     * @param bitbucket the remote bitbucket service
     * @param node      the changeset node id
     * @param repository repository
     * @return the parsed {@link com.atlassian.jira.plugins.bitbucket.api.Changeset}
     */
    public static Changeset load(Communicator bitbucket, SourceControlRepository repository, String node)
    {
        return new LazyLoadedBitbucketChangeset(bitbucket, repository, node);
    }

    /**
     * Parse the json object as a bitbucket changeset
     *
     * @param json  the json object describing the change
     * @return the parsed {@link com.atlassian.jira.plugins.bitbucket.api.Changeset}
     */
    public static Changeset parse(int repositoryId, String branch, JSONObject commitJson)
    {
        try
        {
            JSONObject author = commitJson.getJSONObject("author");
            return new DefaultBitbucketChangeset(
                    repositoryId,
                    commitJson.getString("id"),
                    author.has("name") ? author.getString("name") : "",
                    author.has("login") ? author.getString("login") : "",
                    parseDate(commitJson.getString("authored_date")),
                    "", // todo: raw-node. what is it in github?
                    branch,
                    commitJson.getString("message"),
                    stringList(commitJson.getJSONArray("parents")),
                    fileList(commitJson)
            );
        }

        catch (JSONException e)
        {
            throw new SourceControlException("invalid json object", e);
        }
    }

    /**
     * Parse the json object from github post-commit hook as a bitbucket changeset
     *
     * @param json  the json object describing the change
     * @return the parsed {@link com.atlassian.jira.plugins.bitbucket.api.Changeset}
     */
    public static Changeset parseFromPostcommitHook(int repositoryId, JSONObject commitJson)
    {
        try
        {
            JSONObject author = commitJson.getJSONObject("author");
            return new DefaultBitbucketChangeset(
                    repositoryId,
                    commitJson.getString("id"),
                    author.has("name") ? author.getString("name") : "",
                    author.has("login") ? author.getString("login") : "",
                    parseDate(commitJson.getString("timestamp")),
                    "",
                    "",
                    commitJson.getString("message"),
                    Collections.<String>emptyList(),
                    Collections.<ChangesetFile>emptyList()
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


    private static List<String> stringList(JSONArray parents) throws JSONException
    {
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < parents.length(); i++)
            list.add(((JSONObject) parents.get(i)).getString("id"));
        return list;
    }

    private static List<ChangesetFile> fileList(JSONObject commitJson) throws JSONException
    {
        List<ChangesetFile> list = new ArrayList<ChangesetFile>();

        if (commitJson.has("added"))
        {
            JSONArray arrayAdded = commitJson.getJSONArray("added");
            for (int i = 0; i < arrayAdded.length(); i++)
            {
                String addFilename = arrayAdded.getString(i);
                list.add(new DefaultBitbucketChangesetFile(ChangesetFileAction.ADDED, addFilename));
            }
        }
        if (commitJson.has("removed"))
        {
            JSONArray arrayRemoved = commitJson.getJSONArray("removed");
            for (int i = 0; i < arrayRemoved.length(); i++)
            {
                String remFilename = arrayRemoved.getString(i);
                list.add(new DefaultBitbucketChangesetFile(ChangesetFileAction.REMOVED, remFilename));
            }
        }

        if (commitJson.has("modified"))
        {
            JSONArray arrayModified = commitJson.getJSONArray("modified");

            for (int i = 0; i < arrayModified.length(); i++)
            {
                String modFilename = arrayModified.getJSONObject(i).getString("filename");
                list.add(new DefaultBitbucketChangesetFile(ChangesetFileAction.MODIFIED, modFilename));
            }

        }

        return list;
    }

    private GithubChangesetFactory()
    {
    }
}
