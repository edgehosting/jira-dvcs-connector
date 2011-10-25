package com.atlassian.jira.plugins.bitbucket.activeobjects;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

/**
 * Uses a plugin Settings Factory to store the state of the plugin
 */
public class DefaultBitbucketProjectSettings implements BitbucketProjectSettings
{
    public static final String PROGRESS_TIP = "tip";
    public static final String PROGRESS_COMPLETED = "completed";
    private final Logger logger = LoggerFactory.getLogger(DefaultBitbucketProjectSettings.class);
    private final PluginSettingsFactory pluginSettingsFactory;

    public DefaultBitbucketProjectSettings(PluginSettingsFactory pluginSettingsFactory)
    {
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    private String getStringProperty(String projectKey, String key)
    {
        return (String) pluginSettingsFactory.createSettingsForKey(projectKey).get(key);
    }

    private void setStringProperty(String projectKey, String key, String value)
    {
        pluginSettingsFactory.createSettingsForKey(projectKey).put(key, value);
    }

    private void removeValue(String projectKey, String key)
    {
        pluginSettingsFactory.createSettingsForKey(projectKey).remove(key);
    }

    private List<String> getStringListValue(String projectKey, String key)
    {
        @SuppressWarnings({"unchecked"})
        List<String> list = (List<String>) pluginSettingsFactory.createSettingsForKey(projectKey).get(key);
        return list == null ? new ArrayList<String>() : list;
    }

    private void setStringListValue(String projectKey, String key, List<String> value)
    {
        if (value != null && !value.isEmpty())
            pluginSettingsFactory.createSettingsForKey(projectKey).put(key, value);
        else
            removeValue(projectKey, key);
    }

    public void startSyncProgress(String projectKey, String repositoryUrl)
    {
        logger.debug("starting sync for [ {} ] at [ {} ]", projectKey, repositoryUrl);
        String key = "currentsync" + repositoryUrl + projectKey;
        String value = PROGRESS_TIP;
        setStringProperty(projectKey, key, value);
    }

    public void setSyncProgress(String projectKey, String repositoryUrl, int revision)
    {
        logger.debug("setting progress for [ {} ] at [ {} ] to [ {} ]", new Object[]{projectKey, repositoryUrl, revision});
        setStringProperty(projectKey, "currentsync" + repositoryUrl + projectKey, String.valueOf(revision));
    }

    public void completeSyncProgress(String projectKey, String repositoryUrl)
    {
        logger.debug("complete progress for [ {} ] at [ {} ]", projectKey, repositoryUrl);
        setStringProperty(projectKey, "currentsync" + repositoryUrl + projectKey, PROGRESS_COMPLETED);
        setStringProperty(projectKey, "bitbucketLastSyncTime" + repositoryUrl, new Date().toString());
    }

    public int getCount(String projectKey, String repositoryUrl, String type)
    {
        String commitCountString = getStringProperty(projectKey, type + repositoryUrl);
        return commitCountString == null ? 0 : Integer.parseInt(commitCountString);
    }

    public void resetCount(String projectKey, String repositoryUrl, String type, int count)
    {
        removeValue(projectKey, type + repositoryUrl);
    }

    public void incrementCommitCount(String projectKey, String repositoryUrl, String type)
    {
        int commitCount = getCount(projectKey, repositoryUrl, type);
        setStringProperty(projectKey, type + repositoryUrl, String.valueOf(commitCount + 1));
    }

    public String getUsername(String projectKey, String repositoryURL)
    {
        return getStringProperty(projectKey, "bitbucketUserName" + repositoryURL);
    }

    public void setUsername(String projectKey, String repositoryURL, String username)
    {
        setStringProperty(projectKey, "bitbucketUserName" + repositoryURL, username);
    }

    public String getPassword(String projectKey, String repositoryURL)
    {
        return getStringProperty(projectKey, "bitbucketPassword" + repositoryURL);
    }

    public void setPassword(String projectKey, String repositoryURL, String password)
    {
        setStringProperty(projectKey, "bitbucketPassword" + repositoryURL, password);
    }

    public List<String> getCommits(String projectKey, String repositoryURL, String issueId)
    {
        return getStringListValue(projectKey, "bitbucketIssueCommitArray" + issueId);
    }

    public void setCommits(String projectKey, String repositoryURL, String issueId, List<String> commits)
    {
        String key = "bitbucketIssueCommitArray" + issueId;
        setStringListValue(projectKey, key, commits);
    }

    public List<String> getIssueIds(String projectKey, String repositoryURL)
    {
        return getStringListValue(projectKey, "bitbucketIssueIDs" + repositoryURL);
    }

    public void setIssueIds(String projectKey, String repositoryURL, List<String> issueIds)
    {
        setStringListValue(projectKey, "bitbucketIssueIDs" + repositoryURL, issueIds);
    }

    public List<String> getRepositories(String projectKey)
    {
        return getStringListValue(projectKey, "bitbucketRepositoryURLArray");
    }

    public void setRepositories(String projectKey, List<String> repositories)
    {
        setStringListValue(projectKey, "bitbucketRepositoryURLArray", repositories);
    }
}
