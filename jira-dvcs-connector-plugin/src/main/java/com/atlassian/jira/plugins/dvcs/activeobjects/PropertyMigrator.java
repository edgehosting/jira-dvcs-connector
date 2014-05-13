package com.atlassian.jira.plugins.dvcs.activeobjects;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.jira.plugins.dvcs.activeobjects.v1.IssueMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v1.ProjectMapping;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 */
@SuppressWarnings("deprecation")
public class PropertyMigrator implements ActiveObjectsUpgradeTask
{
    private final Logger logger = LoggerFactory.getLogger(PropertyMigrator.class);

    private final ProjectManager projectManager;
    private final BitbucketProjectSettings settings;


    public PropertyMigrator(ProjectManager projectManager, BitbucketProjectSettings settings)
    {
        this.projectManager = projectManager;
        this.settings = settings;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void upgrade(ModelVersion modelVersion, final ActiveObjects activeObjects)
    {
        logger.debug("upgrade [ " + modelVersion + " ]");

        //noinspection unchecked
        activeObjects.migrate(IssueMapping.class, ProjectMapping.class);

        List<Project> projects = projectManager.getProjectObjects();
        for (Project project : projects)
        {
            String projectKey = project.getKey();

            List<String> repositories = settings.getRepositories(projectKey);
            for (String repository : repositories)
            {
                String username = settings.getUsername(projectKey, repository);
                String password = settings.getPassword(projectKey, repository);

                String repositoryUri = RepositoryUri.parse(repository).getRepositoryUri();
                logger.debug("migrate repository [ {} ]", repositoryUri);
                Map<String, Object> map = Maps.newHashMap();
                map.put("PROJECT_KEY", projectKey);
                map.put("REPOSITORY_URI", repositoryUri);
                map.put("USERNAME", username);
                map.put("PASSWORD", password);
                activeObjects.create(ProjectMapping.class, map);

                try
                {
                    List<String> issueIds = settings.getIssueIds(projectKey, repository);
                    for (String issueId : issueIds)
                    {
                        List<String> commits = settings.getCommits(projectKey, repository, issueId);
                        for (String commit : commits)
                        {
                            URL changesetURL = new URL(commit);
                            String changesetPath = changesetURL.getPath();
                            String node = changesetPath.substring(changesetPath.lastIndexOf("/") + 1);
                            logger.debug("add changeset [ {} ] to [ {} ]", changesetPath, issueId);

                            Map<String, Object> issueMap = Maps.newHashMap();
                            issueMap.put("NODE", node);
                            issueMap.put("PROJECT_KEY", projectKey);
                            issueMap.put("ISSUE_ID", issueId);
                            issueMap.put("REPOSITORY_URI", repositoryUri);
                            activeObjects.create(IssueMapping.class, issueMap);
                        }
                    }
                }
                catch (MalformedURLException e)
                {
                    logger.error("invalid repository url [ " + repository + " ] was not processed");
                }
            }
        }
        logger.debug("completed property migration");
    }

    @Override
    public ModelVersion getModelVersion()
    {
        return ModelVersion.valueOf("1");
    }

}
