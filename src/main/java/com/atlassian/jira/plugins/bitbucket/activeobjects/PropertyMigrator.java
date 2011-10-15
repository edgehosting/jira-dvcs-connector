package com.atlassian.jira.plugins.bitbucket.activeobjects;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v1.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v1.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryPersister;
import com.atlassian.jira.plugins.bitbucket.api.impl.DefaultRepositoryPersister;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.RepositoryUri;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;

/**
 */
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

    public void upgrade(ModelVersion modelVersion, final ActiveObjects activeObjects)
    {
        logger.debug("upgrade [ " + modelVersion + " ]");

        //noinspection unchecked
        activeObjects.migrate(IssueMapping.class, ProjectMapping.class);

        // why DI doesn't work for this?
    	RepositoryPersister repositoryPersister = new DefaultRepositoryPersister(activeObjects);
    	
        List<Project> projects = projectManager.getProjectObjects();
        for (Project project : projects)
        {
            String projectKey = project.getKey();

            List<String> repositories = settings.getRepositories(projectKey);
            for (String repository : repositories)
            {

                String username = settings.getUsername(projectKey, repository);
                String password = settings.getPassword(projectKey, repository);

                String repositoryUrl = RepositoryUri.parse(repository).getRepositoryUrl(); // re-convert the url in case it's in short format "owner/slug";
                logger.debug("migrate repository [ {} ]", repositoryUrl);
                repositoryPersister.addRepository(projectKey, repositoryUrl, username, password);
                // After addding repository we should synchronise now. 
                // Synchronisation can be triggered from RepositoryManager, 
                // but injecting it into this class doesn't work.
                // Until we come with better solutions users with old data 
                // will have to manually trigger Force Synchronisation
/*
                try
                {
                    String decryptedPassword = encryptor.decrypt(pm.getPassword(), pm.getProjectKey(), pm.getRepositoryUri());
                    DefaultSourceControlRepository repo = new DefaultSourceControlRepository(pm.getID(), RepositoryUri.parse(pm.getRepositoryUri()).getRepositoryUrl(),
							pm.getProjectKey(), pm.getUsername(), decryptedPassword);

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

							Changeset changeset = BitbucketChangesetFactory.load(bitbucket, repo, node);
							repositoryPersister.addChangeset(issueId, changeset);
                        }
                    }
                }
                catch (MalformedURLException e)
                {
                    logger.error("invalid repository url [ " + repository + " ] was not processed");
                }
*/
            }
        }
        logger.debug("completed property migration");
    }

    public ModelVersion getModelVersion()
    {
        return ModelVersion.valueOf("1");
    }

}
