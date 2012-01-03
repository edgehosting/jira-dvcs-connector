package com.atlassian.jira.plugins.bitbucket.spi;

import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.ChangesetFile;
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
    public static final Logger log = LoggerFactory.getLogger(ToChangesetTransformer.class);
    private final RepositoryManager repositoryManager;

    public ToChangesetTransformer(RepositoryManager repositoryManager)
    {
        this.repositoryManager = repositoryManager;
    }

    @Override
    public Changeset apply(IssueMapping issueMapping)
    {
        if (!isLatestVersion(issueMapping))
        {
            return repositoryManager.reloadChangeset(issueMapping);
        }

        FileData fileData = parseFilesData(issueMapping.getFilesData());
        List<String> parents = parseParentsData(issueMapping.getParentsData());

        return new DefaultChangeset(issueMapping.getRepositoryId(),
                issueMapping.getNode(),
                issueMapping.getRawAuthor(),
                issueMapping.getAuthor(),
                issueMapping.getDate(),
                issueMapping.getRawNode(),
                issueMapping.getBranch(),
                issueMapping.getMessage(),
                parents,
                fileData.getFiles(),
                fileData.getFileCount());
    }

    private boolean isLatestVersion(IssueMapping from)
    {
        return from.getVersion() != null && from.getVersion() >= IssueMapping.LATEST_VERSION;
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

                files.add(new DefaultBitbucketChangesetFile(CustomStringUtils.getChangesetFileAction(status),
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
