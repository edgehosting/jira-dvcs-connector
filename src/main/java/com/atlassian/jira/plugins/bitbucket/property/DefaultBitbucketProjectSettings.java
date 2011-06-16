package com.atlassian.jira.plugins.bitbucket.property;

import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Uses a plugin Settings Factory to store the state of the plugin
 */
public class DefaultBitbucketProjectSettings implements BitbucketProjectSettings
{
    private final Logger logger = LoggerFactory.getLogger(DefaultBitbucketProjectSettings.class);
    private final PluginSettingsFactory pluginSettingsFactory;

    public DefaultBitbucketProjectSettings(PluginSettingsFactory pluginSettingsFactory)
    {
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    public BitbucketSyncProgress getSyncProgress(String projectKey, String repositoryUrl)
    {
        return (BitbucketSyncProgress) pluginSettingsFactory.createSettingsForKey(projectKey).get("currentsync" + repositoryUrl + projectKey);
    }

    public void startSyncProgress(String projectKey, String repositoryUrl)
    {
        logger.debug("starting sync for [ {} ] at [ {} ]", projectKey, repositoryUrl);
        pluginSettingsFactory.createSettingsForKey(projectKey).put("currentsync" + repositoryUrl + projectKey, new BitbucketSyncProgress.NotStarted());
    }

    public void setSyncProgress(String projectKey, String repositoryUrl, int revision)
    {
        logger.debug("setting progress for [ {} ] at [ {} ] to [ {} ]", new Object[]{projectKey, repositoryUrl, revision});
        pluginSettingsFactory.createSettingsForKey(projectKey).put("currentsync" + repositoryUrl + projectKey, new BitbucketSyncProgress.InProgress(revision));
    }

    public void completeSyncProgress(String projectKey, String repositoryUrl)
    {
        logger.debug("complete progress for [ {} ] at [ {} ]", projectKey, repositoryUrl);
        pluginSettingsFactory.createSettingsForKey(projectKey).put("currentsync" + repositoryUrl + projectKey, new BitbucketSyncProgress.Completed());
        pluginSettingsFactory.createSettingsForKey(projectKey).put("bitbucketLastSyncTime" + repositoryUrl, new Date().toString());
    }

    public int getCount(String projectKey, String repositoryUrl, String type)
    {

    }

    public void incrementCommitCount(String projectKey, String repositoryUrl, String type)
    {
        int commitCount;
        if (pluginSettingsFactory.createSettingsForKey(projectKey).get(type + repositoryUrl) == null)
            commitCount = 0;
        else
            commitCount = Integer.parseInt((String) pluginSettingsFactory.createSettingsForKey(projectKey).get(type + repositoryUrl)) + 1;

        pluginSettingsFactory.createSettingsForKey(projectKey).put(type + repositoryUrl, Integer.toString(commitCount));
        return commitCount;
    }

    public String getUsername(String projectKey, String repositoryURL)
    {
        return (String) pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketUserName" + repositoryURL);

    }

    public String getPassword(String projectKey, String repositoryURL)
    {
        return (String) pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketPassword" + repositoryURL);
    }
}
