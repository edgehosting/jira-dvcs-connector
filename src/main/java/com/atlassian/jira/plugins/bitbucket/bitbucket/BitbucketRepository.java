package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.plugins.bitbucket.bitbucket.connection.BitbucketConnection;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

import java.util.*;

/**
 * Describes a repository on bitbucket
 */
public interface BitbucketRepository
{
    public String getWebsite();

    public String getName();

    public int getFollowers();

    public String getOwner();

    public String getLogo();

    public String getResourceUri();

    public String getRepositoryUrl();

    public String getSlug();

    public String getDescription();
}
