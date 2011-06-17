package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

/**
 * Details on a changeset found in Bitbucket.
 */
public class BitbucketChangeset
{
    public static BitbucketChangeset parse(JSONObject json)
    {
        try
        {
            return new BitbucketChangeset(
                    json.getString("node"),
                    json.getString("raw_author"),
                    json.getString("author"),
                    json.getString("timestamp"),
                    json.getString("raw_node"),
                    json.getString("branch"),
                    json.getString("message"),
                    json.getInt("revision")
            );
        }
        catch (JSONException e)
        {
            throw new BitbucketException("invalid json object",e);
        }
    }

    private final String node;
    private final String rawAuthor;
    private final String author;
    private final String timestamp;
    private final String rawNode;
    private final String branch;
    private final String message;
    private final int revision;
    // TODO: file list

    public BitbucketChangeset(String node, String rawAuthor, String author, String timestamp,
                              String rawNode, String branch, String message, int revision)
    {
        this.node = node;
        this.rawAuthor = rawAuthor;
        this.author = author;
        this.timestamp = timestamp;
        this.rawNode = rawNode;
        this.branch = branch;
        this.message = message;
        this.revision = revision;
    }

    public String getNode()
    {
        return node;
    }

    public String getRawAuthor()
    {
        return rawAuthor;
    }

    public String getAuthor()
    {
        return author;
    }

    public String getTimestamp()
    {
        return timestamp;
    }

    public String getRawNode()
    {
        return rawNode;
    }

    public String getBranch()
    {
        return branch;
    }

    public String getMessage()
    {
        return message;
    }

    public int getRevision()
    {
        return revision;
    }
}
