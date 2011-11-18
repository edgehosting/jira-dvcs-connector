package com.atlassian.jira.plugins.bitbucket.spi.github;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.ChangesetFile;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.Communicator;
import com.atlassian.jira.plugins.bitbucket.spi.DefaultBitbucketChangeset;
import com.atlassian.jira.plugins.bitbucket.spi.LazyLoadedBitbucketChangeset;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketChangesetFileFactory;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    public static Changeset parse(int repositoryId, String branch, JSONObject json)
    {
        try
        {
			return new DefaultBitbucketChangeset(
                    repositoryId,
                    json.getString("id"),
                    json.getJSONObject("author").getString("name"),
                    json.getJSONObject("author").getString("login"),
                    parseDate(json.getString("authored_date")),
                    "", // todo: raw-node. what is it in github?
                    branch,
                    json.getString("message"),
                    stringList(json.getJSONArray("parents")),
                    Collections.<ChangesetFile>emptyList() // todo fileList(json.getJSONArray("files"))
            );
        }

        catch (JSONException e)
        {
            throw new SourceControlException("invalid json object", e);
        }
    }

    public static Date parseDate(String dateStr) {
//        // Atom (ISO 8601) example: 2011-11-09T06:24:13-08:00
//        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
//        try {
//            return df.parse(dateStr);
//        } catch (ParseException e) {
//            throw new SourceControlException("Could not parse date string from JSON.", e);
//        }

        //NOTE: SimpleDateFormat uses GMT[-+]hh:mm for the TZ which breaks
        //things a bit.  Before we go on we have to repair this.

        try
        {
            SimpleDateFormat df = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssz" );

            //this is zero time so we need to add that TZ indicator for
            if ( dateStr.endsWith( "Z" ) ) {
                dateStr = dateStr.substring( 0, dateStr.length() - 1) + "GMT-00:00";
            } else {
                int inset = 6;

                String s0 = dateStr.substring( 0, dateStr.length() - inset );
                String s1 = dateStr.substring( dateStr.length() - inset, dateStr.length() );

                dateStr = s0 + "GMT" + s1;
            }
            return df.parse(dateStr);
        } catch (ParseException e)
        {
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

    private static List<ChangesetFile> fileList(JSONArray parents) throws JSONException
    {
        List<ChangesetFile> list = new ArrayList<ChangesetFile>();
        for (int i = 0; i < parents.length(); i++)
            list.add(BitbucketChangesetFileFactory.parse((JSONObject) parents.get(i)));
        return list;
    }

    private GithubChangesetFactory()
    {
    }
}
