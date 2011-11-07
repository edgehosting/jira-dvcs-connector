package com.atlassian.jira.plugins.bitbucket.activeobjects.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.google.common.collect.Maps;

/**
 *  I would be good to check that github plugin is not installed
 */
public class To_06_GithubRepositories implements ActiveObjectsUpgradeTask
{
    private final Logger log = LoggerFactory.getLogger(To_06_GithubRepositories.class);
    private final PluginSettingsFactory pluginSettingsFactory;
    private final ProjectManager projectManager;

    public To_06_GithubRepositories(final ProjectManager projectManager, final PluginSettingsFactory pluginSettingsFactory)
    {
        this.projectManager = projectManager;
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    private List<String> getRepositories(String projectKey)
    {
        List<String> repoUrls = (List<String>) pluginSettingsFactory.createSettingsForKey(projectKey).get("githubRepositoryURLArray");
        //TODO remove this value from the project settings? 
        return repoUrls != null ? repoUrls : Collections.<String> emptyList();
    }

    public void upgrade(ModelVersion currentVersion, ActiveObjects activeObjects)
    {
        log.debug("upgrade [ " + getModelVersion() + " ]");
        activeObjects.migrate(ProjectMapping.class, IssueMapping.class);
        List<Project> projects = projectManager.getProjectObjects();
        for (Project project : projects)
        {
            String projectKey = project.getKey();
            log.debug(" === migrating repositories for project [{}] === ", projectKey);

            List<String> repositoriesUrls = getRepositories(projectKey);
            for (String repositoryUrl : repositoriesUrls)
            {
                log.debug("migrating repository [{}]", repositoryUrl);
                ProjectMapping pm = migrateRepository(activeObjects, projectKey, repositoryUrl);
                migrateIssueMappings(activeObjects, projectKey, repositoryUrl, pm);
            }
        }
    }

    private void migrateIssueMappings(ActiveObjects activeObjects, String projectKey, String repositoryUrl, ProjectMapping pm)
    {
        ArrayList<String> issueIds = (ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("githubIssueIDs" + repositoryUrl);
        if (issueIds != null)
        {
            for (String issueId : issueIds)
            {
                ArrayList<String> commitArray = (ArrayList<String>) pluginSettingsFactory.createSettingsForKey(projectKey).get("githubIssueCommitArray" + issueId);
                for (String commit : commitArray)
                {
                    String node = extractNode(commit);
                    log.debug("migrating commit mapping node:[{}], issueId:[{}]", node, issueId);
                    
                    
                    final Map<String, Object> map = Maps.newHashMap();
                    map.put("REPOSITORY_ID", pm.getID());
                    map.put("NODE", node);
                    map.put("ISSUE_ID", issueId);
                    activeObjects.create(IssueMapping.class, map);
                }
            }
        }
    }

    private ProjectMapping migrateRepository(ActiveObjects activeObjects, String projectKey, String repositoryUrl)
    {
        String fixedUrl = fixUrl(repositoryUrl);
        String access_token = (String) pluginSettingsFactory.createSettingsForKey(projectKey).get("githubRepositoryAccessToken" + repositoryUrl);

        final Map<String, Object> map = Maps.newHashMap();
        map.put("REPOSITORY_URL", fixedUrl);
        map.put("PROJECT_KEY", projectKey);
        map.put("REPOSITORY_TYPE", "github");
        map.put("ACCESS_TOKEN", access_token);
        return activeObjects.create(ProjectMapping.class, map);
    }

    private String extractNode(String commitUrl)
    {
        // "https://github.com/api/v2/json/commits/show/" + path[3] + "/" + path[4] +"/" + commitId + "?branch=" + branch
        return commitUrl.replaceAll("(.*/|\\?.*)", "");
    }
    
    public static void main(String[] args)
    {
        String url = "https://github.com/api/v2/json/commits/show/owner/repo/jh783263h23kh?branch=mybranch";
        String s1 = url.replaceAll("(.*/|\\?.*)", "");
        System.out.println(s1);
    }

    private String fixUrl(String repository)
    {
        // TODO - remove branch Auto-generated method stub
        return repository;
    }

    public ModelVersion getModelVersion()
    {
        return ModelVersion.valueOf("6");
    }
}
