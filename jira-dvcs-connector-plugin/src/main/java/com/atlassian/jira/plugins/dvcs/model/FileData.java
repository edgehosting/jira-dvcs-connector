package com.atlassian.jira.plugins.dvcs.model;

import static org.apache.commons.lang.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import java.util.ArrayList;
import java.util.List;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON representation of Changeset file data.
 */
public class FileData
{
    private static final Logger logger = LoggerFactory.getLogger(FileData.class);

    /**
     * Converts a Changeset's file data into JSON.
     *
     * @param changeset a Changeset
     * @return a JSON string containing the file data
     */
    public static String toJSON(final Changeset changeset)
    {
        JSONObject filesDataJson = new JSONObject();
        try
        {
            int count = changeset.getAllFileCount();
            filesDataJson.put("count", count);

            // there is no need to add the files info in fileData when we already have the detailed version. ideally we
            // would have migrated the AO table into a nicer format but that is perceived as riskier so we just handle
            // it as and when we update the ChangesetMapping
            if (changeset.getFileDetails() == null)
            {
                JSONArray filesJson = new JSONArray();
                List<ChangesetFile> files = changeset.getFiles();
                for (int i = 0; i < Math.min(count, Changeset.MAX_VISIBLE_FILES); i++)
                {
                    ChangesetFile changesetFile = files.get(i);
                    JSONObject fileJson = new JSONObject();
                    fileJson.put("filename", changesetFile.getFile());
                    fileJson.put("status", changesetFile.getFileAction().getAction());

                    filesJson.put(fileJson);
                }

                filesDataJson.put("files", filesJson);
            }

            return filesDataJson.toString();
        }
        catch (JSONException e)
        {
            logger.error("Creating files JSON failed!", e);
        }

        return null;
    }

    /**
     * Creates a FileData for a ChangesetMapping.
     *
     * @param changesetMapping a ChangesetMapping
     * @return a FileData
     */
    public static FileData from(final ChangesetMapping changesetMapping)
    {
        List<ChangesetFile> files = new ArrayList<ChangesetFile>();
        int fileCount = 0;

        String filesData = changesetMapping.getFilesData();
        if (StringUtils.isNotBlank(filesData))
        {
            try
            {
                JSONObject filesDataJson = new JSONObject(filesData);
                fileCount = filesDataJson.getInt("count");

                // prefer to use the files detail data if it is available
                String fileDetailsJson = changesetMapping.getFileDetailsJson();
                if (fileDetailsJson != null)
                {
                    //noinspection ConstantConditions
                    files.addAll(ChangesetFileDetails.fromJSON(fileDetailsJson));
                }
                else
                {
                    JSONArray filesJson = filesDataJson.optJSONArray("files");
                    for (int i = 0; i < filesJson.length(); i++)
                    {
                        JSONObject file = filesJson.getJSONObject(i);
                        String filename = file.getString("filename");
                        String status = file.getString("status");

                        files.add(new ChangesetFile(CustomStringUtils.getChangesetFileAction(status), filename));
                    }
                }
            }
            catch (JSONException e)
            {
                logger.error("Failed parsing files from FileJson data.", e);
            }
        }

        return new FileData(files, fileCount);
    }

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

    @Override
    public boolean equals(final Object obj)
    {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }
}
