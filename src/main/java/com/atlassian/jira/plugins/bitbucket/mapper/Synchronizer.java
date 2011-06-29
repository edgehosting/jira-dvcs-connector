package com.atlassian.jira.plugins.bitbucket.mapper;

import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Synchronization services
 */
public interface Synchronizer
{
    /**
     * Perform a full sync on the specified project with the specified bitbucket repostiory
     * @param projectKey the jira project key
     * @param owner the owner of the bitbucket repository
     * @param slug the slug of the bitbucket repository
     */
    public void synchronize(String projectKey, String owner, String slug);

    public void postReceiveHook(String payload);

}
