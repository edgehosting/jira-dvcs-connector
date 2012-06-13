package com.atlassian.jira.plugins.dvcs.activeobjects;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cleanup of plugin property setting - removing repositories and commits records for BB and GH
 */
@SuppressWarnings("unchecked")
public class CleanupPluginSettings implements ActiveObjectsUpgradeTask
{
    private final Logger log = LoggerFactory.getLogger(CleanupPluginSettings.class);
    private final PluginSettingsFactory pluginSettingsFactory;
    private final ProjectManager projectManager;

    public CleanupPluginSettings(final ProjectManager projectManager, final PluginSettingsFactory pluginSettingsFactory)
    {
        this.projectManager = projectManager;
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    @Override
    public void upgrade(ModelVersion currentVersion, ActiveObjects activeObjects)
    {
        log.debug("upgrade [ " + getModelVersion() + " ]");

        cleanupBitbucketRecords();
        cleanupGithubRecords();
    }

    private void cleanupBitbucketRecords()
    {
        List<Project> projects = projectManager.getProjectObjects();
        for (Project project : projects)
        {
            String projectKey = project.getKey();
            List<String> repositoriesUrls = getBitbucketRepositories(projectKey);
            if (repositoriesUrls != null)
            {
                for (String repositoryUrl : repositoriesUrls)
                {
                    removeBitbucketIssueMappings(projectKey, repositoryUrl);
                    removeBitbucketCredentials(projectKey, repositoryUrl);
                }
            }
            removeBitbucketRepositories(projectKey);
        }
    }

    private List<String> getBitbucketRepositories(String projectKey)
    {
        List<String> repoUrls = (List<String>) pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketRepositoryURLArray");
        return repoUrls != null ? repoUrls : Collections.<String>emptyList();
    }

    private void removeBitbucketRepositories(String projectKey)
    {
        log.debug(" === removing Bitbucket repositories for project [{}] === ", projectKey);
        pluginSettingsFactory.createSettingsForKey(projectKey).remove("githubRepositoryURLArray");
    }

    private void removeBitbucketIssueMappings(String projectKey, String repositoryUrl)
    {
        log.debug(" === removing issue mappings for Bitbucket repository {} === ", repositoryUrl);
        PluginSettings ps = pluginSettingsFactory.createSettingsForKey(projectKey);
        ArrayList<String> issueIds = (ArrayList<String>) ps.get("bitbucketIssueIDs" + repositoryUrl);
        if (issueIds != null)
        {
            for (String issueId : issueIds)
            {
                ps.remove("bitbucketIssueCommitArray" + issueId);
            }
        }
    }

    private void removeBitbucketCredentials(String projectKey, String repositoryUrl)
    {
        log.debug(" === removing credentials for Bitbucket repository {} === ", repositoryUrl);
        PluginSettings ps = pluginSettingsFactory.createSettingsForKey(projectKey);
        ps.remove("bitbucketUserName" + repositoryUrl);
        ps.remove("bitbucketPassword" + repositoryUrl);
    }


    private void cleanupGithubRecords()
    {
        List<Project> projects = projectManager.getProjectObjects();
        for (Project project : projects)
        {
            String projectKey = project.getKey();
            List<String> repositoriesUrls = getGithubRepositories(projectKey);
            if (repositoriesUrls != null)
            {
                for (String repositoryUrl : repositoriesUrls)
                {
                    removeGithubIssueMappings(projectKey, repositoryUrl);
                }
            }
            removeGithubRepositories(projectKey);
        }
    }

    private List<String> getGithubRepositories(String projectKey)
    {
        List<String> repoUrls = (List<String>) pluginSettingsFactory.createSettingsForKey(projectKey).get("githubRepositoryURLArray");
        return repoUrls != null ? repoUrls : Collections.<String>emptyList();
    }

    private void removeGithubRepositories(String projectKey)
    {
        log.debug(" === removing Github repositories for project [{}] === ", projectKey);
        pluginSettingsFactory.createSettingsForKey(projectKey).remove("githubRepositoryURLArray");
    }

    private void removeGithubIssueMappings(String projectKey, String repositoryUrl)
    {
        log.debug(" === removing issue mappings for Github repository {} === ", repositoryUrl);
        PluginSettings ps = pluginSettingsFactory.createSettingsForKey(projectKey);
        ArrayList<String> issueIds = (ArrayList<String>) ps.get("githubIssueIDs" + repositoryUrl);
        if (issueIds != null)
        {
            for (String issueId : issueIds)
            {
                ps.remove("githubIssueCommitArray" + issueId);
            }
        }
    }


    @Override
    public ModelVersion getModelVersion()
    {
        return ModelVersion.valueOf("7");
    }
}
