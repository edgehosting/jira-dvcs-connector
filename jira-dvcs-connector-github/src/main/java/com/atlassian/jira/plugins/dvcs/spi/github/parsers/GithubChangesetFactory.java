package com.atlassian.jira.plugins.dvcs.spi.github.parsers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.RepositoryCommit;

import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.util.concurrent.Nullable;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * Factory for {@link com.atlassian.jira.plugins.bitbucket.api.Changeset} implementations
 */
public class GithubChangesetFactory
{

    /**
     * Parse the json object from GitHub v2. API as a changeset. We need only minimal information from that.
     *
     * @param repositoryId repositoryId
     * @param commitJson commitJson
     * @return the parsed {@link com.atlassian.jira.plugins.bitbucket.api.Changeset} with minimal fields
     * @throws com.atlassian.jira.util.json.JSONException
     */
    public static Changeset parseV2(int repositoryId, JSONObject commitJson) throws JSONException
    {

        String id = commitJson.getString("id");
        String msg = commitJson.getString("message");
        Date date = parseDate(commitJson.getString("committed_date"));

        return new Changeset(repositoryId, id, msg, date);
    }


    /**
     * Parse the json object from GitHub v3. API as a changeset
     *
     * @param repositoryId repositoryId
     * @param branch branch
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

            return new Changeset(
                    repositoryId,
                    json.getString("sha"),
                    "",
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

            list.add(new ChangesetFile(CustomStringUtils.getChangesetFileAction(status),
                    filename, additions, deletions));
        }

        return list;
    }


    private GithubChangesetFactory()
    {
    }
    
    public static Changeset transform(RepositoryCommit repositoryCommit, int repositoryId, String branch)
    {
        final List<ChangesetFile> changesetFiles = transformFiles(repositoryCommit.getFiles());

        return new Changeset(
                repositoryId,
                repositoryCommit.getSha(),
                "",
                repositoryCommit.getCommit().getAuthor().getName(),
                repositoryCommit.getAuthor().getLogin(),
                repositoryCommit.getCommit().getAuthor().getDate(),
                "", // todo: raw-node. what is it in github?
                branch,
                repositoryCommit.getCommit().getMessage(),
                transformParents(repositoryCommit.getParents()),
                changesetFiles,
                changesetFiles.size()
        );
        
    }

    private static List<ChangesetFile> transformFiles(List<CommitFile> files)
    {
        if (files == null)
        {
            return Collections.<ChangesetFile>emptyList();
        }

        return Lists.transform(files, new Function<CommitFile, ChangesetFile>()
        {
            @Override
            public ChangesetFile apply(@Nullable CommitFile commitFile)
            {
                String filename = commitFile.getFilename();
                String status = commitFile.getStatus();
                int additions = commitFile.getAdditions();
                int deletions = commitFile.getDeletions();

                return new ChangesetFile(CustomStringUtils.getChangesetFileAction(status),
                        filename, additions, deletions);
            }
        });
    }

    private static List<String> transformParents(List<Commit> parents)
    {
        if (parents == null)
        {
            return Collections.<String>emptyList();
        }


        return Lists.transform(parents, new Function<Commit, String>()
        {
            @Override
            public String apply(@Nullable Commit commit)
            {
                return commit.getSha();
            }
        });
    }
    
}
