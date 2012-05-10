package com.atlassian.jira.plugins.dvcs.dao.impl.transform;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ChangesetTransformer
{
    public static final Logger log = LoggerFactory.getLogger(ChangesetTransformer.class);

    public Changeset transform(ChangesetMapping changesetMapping)
    {

        if (changesetMapping == null) {
            return null;
        }

        // TODO: reload from DVCS if old version
//        if (!isLatestVersion(issueMapping))
//        {
//            return repositoryManager.reloadChangeset(issueMapping.getRepositoryId(), issueMapping.getNode(), issueMapping.getIssueId(), issueMapping.getBranch());
//        }

        FileData fileData = parseFilesData(changesetMapping.getFilesData());
        List<String> parents = parseParentsData(changesetMapping.getParentsData());

        return new Changeset(changesetMapping.getRepositoryId(),
                changesetMapping.getNode(),
                changesetMapping.getIssueKey(),
                changesetMapping.getRawAuthor(),
                changesetMapping.getAuthor(),
                changesetMapping.getDate(),
                changesetMapping.getRawNode(),
                changesetMapping.getBranch(),
                changesetMapping.getMessage(),
                parents,
                fileData.getFiles(),
                fileData.getFileCount());
    }

    private boolean isLatestVersion(ChangesetMapping changesetMapping)
    {
        return changesetMapping.getVersion() != null && changesetMapping.getVersion() >= ChangesetMapping.LATEST_VERSION;
    }

    private List<String> parseParentsData(String parentsData)
    {
        List<String> parents = new ArrayList<String>();

        if (StringUtils.isBlank(parentsData))
        {
            return parents;
        }

        try
        {
            JSONArray parentsJson = new JSONArray(parentsData);
            for (int i = 0; i < parentsJson.length(); i++)
            {
                parents.add(parentsJson.getString(i));
            }
        } catch (JSONException e)
        {
            log.error("Failed parsing parents from ParentsJson data.");
        }

        return parents;
    }

    private FileData parseFilesData(String filesData)
    {
        List<ChangesetFile> files = new ArrayList<ChangesetFile>();
        int fileCount = 0;

        try
        {
            JSONObject filesDataJson = new JSONObject(filesData);
            fileCount = filesDataJson.getInt("count");
            JSONArray filesJson = filesDataJson.getJSONArray("files");

            for (int i = 0; i < filesJson.length(); i++)
            {
                JSONObject file = filesJson.getJSONObject(i);
                String filename = file.getString("filename");
                String status = file.getString("status");
                int additions = file.getInt("additions");
                int deletions = file.getInt("deletions");

                files.add(new ChangesetFile(CustomStringUtils.getChangesetFileAction(status),
                        filename, additions, deletions));
            }

        } catch (JSONException e)
        {
            log.error("Failed parsing files from FileJson data.");
        }

        return new FileData(files, fileCount);
    }



    private static class FileData
    {
        private final List<ChangesetFile> files;
        private final int fileCount;

        FileData(List<ChangesetFile> files, int fileCount)
        {
            this.files = files;
            this.fileCount = fileCount;
        }

        public List<ChangesetFile> getFiles()
        {
            return files;
        }

        public int getFileCount()
        {
            return fileCount;
        }

    }
}
