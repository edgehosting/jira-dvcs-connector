package com.atlassian.jira.plugins.dvcs.spi.bitbucket.parsers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

/**
 * Factory for {@link Changeset} implementations
 */
public class BitbucketChangesetFactory
{
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final Pattern HGRC_MAIL_PATTERN = Pattern.compile("\\<(.*)\\>");

    static
    {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("utc"));
    }

    /**
     * Parse the json object as a bitbucket changeset
     *
     * @param repositoryId
     * @param baseJson
     * @return the parsed {@link Changeset}
     */
    public static Changeset parse(int repositoryId, JSONObject baseJson)
    {
        try
        {
            List<ChangesetFile> files = fileListFromBaseJson(baseJson);
            String rawAuthor = baseJson.getString("raw_author");
            String authorEmail = parseUsersEmailHgRcFormatOrNull(rawAuthor);

			Changeset changeset = new Changeset(
                    repositoryId,
                    baseJson.getString("node"),
                    null,
                    rawAuthor,
                    baseJson.getString("author"),
                    parseDate(baseJson.getString("utctimestamp")),
                    baseJson.getString("raw_node"),
                    baseJson.getString("branch"),      // TODO if null, set to "default"?
                    baseJson.getString("message"),
                    stringList(baseJson.getJSONArray("parents")),
                    files,
                    files.size()
            );
			
			changeset.setAuthorEmail(authorEmail);

			return changeset;

        } catch (JSONException e)
        {
            throw new SourceControlException("Invalid json object: " + baseJson.toString(), e);
        }
    }

    private static String parseUsersEmailHgRcFormatOrNull(String rawAuthor)
	{
		try
		{
			Matcher matcher = HGRC_MAIL_PATTERN.matcher(rawAuthor);
			matcher.find();
			return matcher.group(1).trim();
		} catch (Exception e)
		{
			// nop
			return null;
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

    public static List<ChangesetFile> fileListFromBaseJson(JSONObject baseJson) throws JSONException
    {
        List<ChangesetFile> list = new ArrayList<ChangesetFile>();
        JSONArray parents = baseJson.getJSONArray("files");
        for (int i = 0; i < parents.length(); i++)
            list.add(BitbucketChangesetFileFactory.parseFromBaseJson((JSONObject) parents.get(i)));
        return list;
    }

    public static List<ChangesetFile> fileListFromDiffstatJson(JSONArray diffstatJson) throws JSONException
    {
        List<ChangesetFile> list = new ArrayList<ChangesetFile>();
        JSONArray parents = diffstatJson;
        for (int i = 0; i < parents.length(); i++)
            list.add(BitbucketChangesetFileFactory.parseFromDiffstatJson((JSONObject) parents.get(i)));

        return list;
    }

    public static Changeset getChangesetWithStatistics(Changeset changeset, String responseFilesString)
    {
        try
        {
            List<ChangesetFile> files = fileListFromDiffstatJson(new JSONArray(responseFilesString));
            Changeset changezet = new Changeset(changeset.getRepositoryId(), changeset.getNode(), null ,changeset.getRawAuthor(), changeset.getAuthor(), changeset.getDate(), changeset.getRawNode(), changeset.getBranch(), changeset.getMessage(), changeset.getParents(), files, changeset.getAllFileCount());
            changezet.setAuthorEmail(changeset.getAuthorEmail());
            return changezet;
        } catch (JSONException e)
        {
            throw new SourceControlException("Invalid diffstat json object: " + responseFilesString, e);
        }
    }

    private BitbucketChangesetFactory()
    {
    }
}
