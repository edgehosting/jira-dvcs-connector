package com.atlassian.jira.plugins.bitbucket.property;

import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    public BitbucketSyncProgress getSyncProgress(String projectKey, String repositoryUrl)
    {
        String progress = (String) pluginSettingsFactory.createSettingsForKey(projectKey).get("currentsync" + repositoryUrl + projectKey);

        if (PROGRESS_COMPLETED.equals(progress))
            return BitbucketSyncProgress.completed();

        if (PROGRESS_TIP.equals(progress))
            return BitbucketSyncProgress.tip();

        return BitbucketSyncProgress.progress(Integer.parseInt(progress));
    }

    public void startSyncProgress(String projectKey, String repositoryUrl)
    {
        logger.debug("starting sync for [ {} ] at [ {} ]", projectKey, repositoryUrl);
        pluginSettingsFactory.createSettingsForKey(projectKey).put("currentsync" + repositoryUrl + projectKey, PROGRESS_TIP);
    }

    public void setSyncProgress(String projectKey, String repositoryUrl, int revision)
    {
        logger.debug("setting progress for [ {} ] at [ {} ] to [ {} ]", new Object[]{projectKey, repositoryUrl, revision});
        pluginSettingsFactory.createSettingsForKey(projectKey).put("currentsync" + repositoryUrl + projectKey, String.valueOf(revision));
    }

    public void completeSyncProgress(String projectKey, String repositoryUrl)
    {
        logger.debug("complete progress for [ {} ] at [ {} ]", projectKey, repositoryUrl);
        pluginSettingsFactory.createSettingsForKey(projectKey).put("currentsync" + repositoryUrl + projectKey, PROGRESS_COMPLETED);
        pluginSettingsFactory.createSettingsForKey(projectKey).put("bitbucketLastSyncTime" + repositoryUrl, new Date().toString());
    }

    public int getCount(String projectKey, String repositoryUrl, String type)
    {
        String commitCountString = (String) pluginSettingsFactory.createSettingsForKey(projectKey).get(type + repositoryUrl);
        return commitCountString == null ? 0 : Integer.parseInt(commitCountString);
    }

    public void resetCount(String projectKey, String repositoryUrl, String type, int count)
    {
        pluginSettingsFactory.createSettingsForKey(projectKey).remove(type + repositoryUrl);
    }

    public void incrementCommitCount(String projectKey, String repositoryUrl, String type)
    {
        int commitCount = getCount(projectKey, repositoryUrl, type);
        pluginSettingsFactory.createSettingsForKey(projectKey).put(type + repositoryUrl, String.valueOf(commitCount + 1));
    }

    public String getUsername(String projectKey, String repositoryURL)
    {
        return (String) pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketUserName" + repositoryURL);
    }

    public void setUsername(String projectKey, String repositoryURL, String username)
    {
        pluginSettingsFactory.createSettingsForKey(projectKey).put("bitbucketUserName" + repositoryURL, username);
    }

    public String getPassword(String projectKey, String repositoryURL)
    {
        return (String) pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketPassword" + repositoryURL);
    }

    public void setPassword(String projectKey, String repositoryURL, String password)
    {
        pluginSettingsFactory.createSettingsForKey(projectKey).put("bitbucketPassword" + repositoryURL, password);
    }

    public List<String> getCommits(String projectKey, String repositoryURL, String issueId)
    {
        List<String> list = (List<String>) pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketIssueCommitArray" + issueId);
        return list == null ? new ArrayList<String>() : list;
    }

    public void setCommits(String projectKey, String repositoryURL, String issueId, List<String> commits)
    {
        if (commits != null && !commits.isEmpty())
            pluginSettingsFactory.createSettingsForKey(projectKey).put("bitbucketIssueCommitArray" + issueId, commits);
        else
            pluginSettingsFactory.createSettingsForKey(projectKey).remove("bitbucketIssueCommitArray" + issueId);
    }

    public List<String> getIssueIds(String projectKey, String repositoryURL)
    {
        List<String> list = (List<String>) pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketIssueIDs" + repositoryURL);
        return list == null ? new ArrayList<String>() : list;
    }

    public void setIssueIds(String projectKey, String repositoryURL, List<String> issueIds)
    {
        if (issueIds != null && !issueIds.isEmpty())
            pluginSettingsFactory.createSettingsForKey(projectKey).put("bitbucketIssueIDs" + repositoryURL, issueIds);
        else
            pluginSettingsFactory.createSettingsForKey(projectKey).remove("bitbucketIssueIDs" + repositoryURL);
    }

    public List<String> getRepositories(String projectKey)
    {
        List<String> list = (List<String>) pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketRepositoryURLArray");
        return list == null ? new ArrayList<String>() : list;
    }

    public void setRepositories(String projectKey, List<String> repositories)
    {
        if (repositories != null && !repositories.isEmpty())
            pluginSettingsFactory.createSettingsForKey(projectKey).put("bitbucketRepositoryURLArray", repositories);
        else
            pluginSettingsFactory.createSettingsForKey(projectKey).remove("bitbucketRepositoryURLArray");
    }
}
