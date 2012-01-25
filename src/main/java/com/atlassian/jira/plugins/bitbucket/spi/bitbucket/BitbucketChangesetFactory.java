package com.atlassian.jira.plugins.bitbucket.spi.bitbucket;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.ChangesetFile;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.spi.DefaultChangeset;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Factory for {@link Changeset} implementations
 */
public class BitbucketChangesetFactory
{
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static
    {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("utc"));
    }

    /**
     * Parse the json object as a bitbucket changeset
     *
     * @param owner the owner of the repository this changeset belongs to
     * @param slug  the slug of the repository this changeset belons to
     * @param json  the json object describing the change
     * @return the parsed {@link Changeset}
     */
    public static Changeset parse(int repositoryId, JSONObject baseJson, JSONArray filesJson)
    {
        try
        {
            List<ChangesetFile> files = fileList(baseJson, filesJson);
            return new DefaultChangeset(
                    repositoryId,
                    baseJson.getString("node"),
                    baseJson.getString("raw_author"),
                    baseJson.getString("author"),
                    parseDate(baseJson.getString("utctimestamp")),
                    baseJson.getString("raw_node"),
                    baseJson.getString("branch"),      // TODO if null, set to "default"?
                    baseJson.getString("message"),
                    stringList(baseJson.getJSONArray("parents")),
                    files,
                    files.size()
            );
        } catch (JSONException e)
        {
            throw new SourceControlException("Invalid json object: " + baseJson.toString(), e);
        }
    }

    public static Date parseDate(String dateStr)
    {
        // example:    2011-05-26 10:54:41+xx:xx   (timezone is ignored because we parse utc timestamp date with utc parser)
        try
        {
            return DATE_FORMAT.parse(dateStr);
        } catch (ParseException e)
        {
            throw new SourceControlException("Could not parse date string from JSON.", e);
        }
    }

    public static String getDateString(Date datetime)
    {
        // example:    2011-05-26 10:54:41
        return DATE_FORMAT.format(datetime);
    }

    private static List<String> stringList(JSONArray parents) throws JSONException
    {
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < parents.length(); i++)
            list.add((String) parents.get(i));
        return list;
    }

    private static List<ChangesetFile> fileList(JSONObject baseJson, JSONArray diffstatJson) throws JSONException
    {
        List<ChangesetFile> list = new ArrayList<ChangesetFile>();
        JSONArray parents;
        if (diffstatJson == null || diffstatJson.length() == 0)
        {
            parents = baseJson.getJSONArray("files");
            for (int i = 0; i < parents.length(); i++)
                list.add(BitbucketChangesetFileFactory.parseFromBaseJson((JSONObject) parents.get(i)));
        } else
        {
            parents = diffstatJson;
            for (int i = 0; i < parents.length(); i++)
                list.add(BitbucketChangesetFileFactory.parseFromDiffstatJson((JSONObject) parents.get(i)));
        }
        return list;
    }

    private BitbucketChangesetFactory()
    {
    }
}
