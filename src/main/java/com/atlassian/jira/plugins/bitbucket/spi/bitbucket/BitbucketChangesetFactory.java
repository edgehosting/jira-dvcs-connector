package com.atlassian.jira.plugins.bitbucket.spi.bitbucket;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.ChangesetFile;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.Communicator;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl.DefaultBitbucketChangeset;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl.LazyLoadedBitbucketChangeset;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Factory for {@link Changeset} implementations
 */
public class BitbucketChangesetFactory {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    /**
     * Load the changeset details based on the authentication method, the repository owner, repository
     * slug, and changeset node id
     *
     * @param bitbucket  the remote bitbucket service
     * @param auth       the authentication method
     * @param owner      the owner of the repository
     * @param slug       the slug of the repository
     * @param node       the changeset node id
     * @param repository
     * @return the parsed {@link Changeset}
     */
    public static Changeset load(Communicator bitbucket, SourceControlRepository repository, String node) {
        return new LazyLoadedBitbucketChangeset(bitbucket, repository, node);
    }

    /**
     * Parse the json object as a bitbucket changeset
     *
     * @param owner the owner of the repository this changeset belongs to
     * @param slug  the slug of the repository this changeset belons to
     * @param json  the json object describing the change
     * @return the parsed {@link Changeset}
     */
    public static Changeset parse(int repositoryId, JSONObject json) {
        try {
            return new DefaultBitbucketChangeset(
                    repositoryId,
                    json.getString("node"),
                    json.getString("raw_author"),
                    json.getString("author"),
                    getDate(json.getString("timestamp")),
                    json.getString("raw_node"),
                    json.getString("branch"),
                    json.getString("message"),
                    stringList(json.getJSONArray("parents")),
                    fileList(json.getJSONArray("files"))
            );
        } catch (JSONException e) {
            throw new SourceControlException("invalid json object", e);
        }
    }

    public static Date getDate(String dateStr) {
        // example:    2011-05-26 10:54:41
        try {
            return DATE_FORMAT.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getDateString(Date datetime) {
        // example:    2011-05-26 10:54:41
        return DATE_FORMAT.format(datetime);
    }

    private static List<String> stringList(JSONArray parents) throws JSONException {
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < parents.length(); i++)
            list.add((String) parents.get(i));
        return list;
    }

    private static List<ChangesetFile> fileList(JSONArray parents) throws JSONException {
        List<ChangesetFile> list = new ArrayList<ChangesetFile>();
        for (int i = 0; i < parents.length(); i++)
            list.add(BitbucketChangesetFileFactory.parse((JSONObject) parents.get(i)));
        return list;
    }

    private BitbucketChangesetFactory() {
    }
}
