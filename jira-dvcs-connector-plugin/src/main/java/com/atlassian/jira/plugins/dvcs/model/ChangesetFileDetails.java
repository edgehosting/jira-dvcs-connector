package com.atlassian.jira.plugins.dvcs.model;

import java.io.IOException;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used for writing file details to/from JSON.
 */
public class ChangesetFileDetails
{
    private static final Logger logger = LoggerFactory.getLogger(ChangesetFileDetails.class);

    private static final TypeReference<List<ChangesetFileDetail>> FILE_DETAIL_JSON_TYPE = new TypeReference<List<ChangesetFileDetail>>() {};
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Writes the file details to JSON.
     *
     * @param fileDetails
     * @return
     */
    public static String toJSON(List<ChangesetFileDetail> fileDetails)
    {
        if (fileDetails != null)
        {
            try
            {
                return objectMapper.writeValueAsString(fileDetails);
            }
            catch (IOException e)
            {
                logger.error("Error writing to JSON: " + fileDetails, e);
            }
        }

        return null;
    }

    /**
     * Reads file details from JSON.
     *
     * @param fileDetailsJson
     * @return
     */
    public static List<ChangesetFileDetail> fromJSON(String fileDetailsJson)
    {
        if (fileDetailsJson != null)
        {
            try
            {
                return objectMapper.readValue(fileDetailsJson, FILE_DETAIL_JSON_TYPE);
            }
            catch (IOException e)
            {
                logger.error("Error reading JSON: " + fileDetailsJson, e);
            }
        }

        return null;
    }
}
