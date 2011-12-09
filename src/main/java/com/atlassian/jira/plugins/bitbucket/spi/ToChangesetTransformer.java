package com.atlassian.jira.plugins.bitbucket.spi;

import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.ChangesetFile;
import com.atlassian.jira.plugins.bitbucket.api.ChangesetFileAction;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.google.common.base.Function;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ToChangesetTransformer implements Function<IssueMapping, Changeset>
{
    private static final Logger LOG = LoggerFactory.getLogger(ToChangesetTransformer.class);
    private RepositoryManager repositoryManager;

    public ToChangesetTransformer(RepositoryManager repositoryManager)
    {
        this.repositoryManager = repositoryManager;
    }

    @Override
    public Changeset apply(IssueMapping issueMapping)
    {
        if (hasOldVersion(issueMapping)) {
            Changeset changeset = loadAndStoreChangeset(issueMapping);
            return changeset;
        }

        FileData fileData = parseFilesData(issueMapping.getFilesData());
        List<String> parents = parseParentsData(issueMapping.getParentsData());

        return new DefaultBitbucketChangeset(issueMapping.getRepositoryId(),
                issueMapping.getNode(),
                issueMapping.getRawAuthor(),
                issueMapping.getAuthor(),
                issueMapping.getTimestamp(),
                issueMapping.getRawNode(),
                issueMapping.getBranch(),
                issueMapping.getMessage(),
                parents,
                fileData.getFiles(),
                fileData.getFileCount());
    }

    private Changeset loadAndStoreChangeset(IssueMapping from)
    {
        return repositoryManager.updateChangeset(from);
    }

    private boolean hasOldVersion(IssueMapping from)
    {
        return from.getVersion() == null || from.getVersion() < IssueMapping.VERSION;
    }

    private List<String> parseParentsData(String parentsData)
    {
        List<String> parents = new ArrayList<String>();

        if (StringUtils.isBlank(parentsData)) {
            return parents;
        }

        try
        {
            JSONArray parentsJson = new JSONArray(parentsData);
            for (int i=0; i<parentsJson.length(); i++) {
                parents.add(parentsJson.getString(i));
            }
        } catch (JSONException e)
        {
            LOG.error("Failed parsing parents from ParentsJson data.");
        }

        return parents;
    }

    private FileData parseFilesData(String filesData) {

        List<ChangesetFile> files = new ArrayList<ChangesetFile>();
        int fileCount = 0;

        try
        {
            JSONObject filesJson = new JSONObject(filesData);
            fileCount = filesJson.getInt("count");
            JSONArray added = filesJson.getJSONArray("added");
            for (int i = 0; i < added.length(); i++)
            {
                String addFilename = added.getString(i);
                files.add(new DefaultBitbucketChangesetFile(ChangesetFileAction.ADDED, addFilename));
            }
            JSONArray removed = filesJson.getJSONArray("removed");
            for (int i = 0; i < removed.length(); i++)
            {
                String removedFilename = removed.getString(i);
                files.add(new DefaultBitbucketChangesetFile(ChangesetFileAction.REMOVED, removedFilename));
            }
            JSONArray modified = filesJson.getJSONArray("modified");
            for (int i = 0; i < modified.length(); i++)
            {
                String modifiedFilename = modified.getString(i);
                files.add(new DefaultBitbucketChangesetFile(ChangesetFileAction.MODIFIED, modifiedFilename));
            }
        } catch (JSONException e)
        {
            LOG.error("Failed parsing files from FileJson data.");
        }

        return new FileData(files, fileCount);
    }


    class FileData {
        private List<ChangesetFile> files;
        private int fileCount;

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
