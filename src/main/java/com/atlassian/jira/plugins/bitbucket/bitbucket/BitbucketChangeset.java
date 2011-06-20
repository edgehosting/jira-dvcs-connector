package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

/**
 * Details on a changeset found in Bitbucket.
 */
public interface BitbucketChangeset
{
    public String getNode();

    public String getRawAuthor();

    public String getAuthor();

    public String getTimestamp();

    public String getRawNode();

    public String getBranch();

    public String getMessage();

    public int getRevision();
}
