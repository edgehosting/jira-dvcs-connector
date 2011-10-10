package com.atlassian.jira.plugins.bitbucket.property;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v1.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v1.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.bitbucket.Authentication;
import com.atlassian.jira.plugins.bitbucket.bitbucket.Bitbucket;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketChangesetFactory;
import com.atlassian.jira.plugins.bitbucket.bitbucket.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.common.Changeset;
import com.atlassian.jira.plugins.bitbucket.mapper.Encryptor;
import com.atlassian.jira.plugins.bitbucket.mapper.RepositoryPersister;
import com.atlassian.jira.plugins.bitbucket.mapper.impl.DefaultRepositoryPersister;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;

/**
 */
public class PropertyMigrator implements ActiveObjectsUpgradeTask
{
    private final Logger logger = LoggerFactory.getLogger(PropertyMigrator.class);

    private final ProjectManager projectManager;
    private final BitbucketProjectSettings settings;
    private final Bitbucket bitbucket;

	private final Encryptor encryptor;

    public PropertyMigrator(ProjectManager projectManager, BitbucketProjectSettings settings,
                            Bitbucket bitbucket, Encryptor encryptor)
    {
        this.projectManager = projectManager;
        this.settings = settings;
        this.bitbucket = bitbucket;
		this.encryptor = encryptor;
    }

    public void upgrade(ModelVersion modelVersion, final ActiveObjects activeObjects)
    {
        logger.debug("upgrade [ " + modelVersion + " ]");

        //noinspection unchecked
        activeObjects.migrate(IssueMapping.class, ProjectMapping.class);

        // why DI doesn't work for this?
    	RepositoryPersister repositoryPersister = new DefaultRepositoryPersister(activeObjects, encryptor);
    	
        List<Project> projects = projectManager.getProjectObjects();
        for (Project project : projects)
        {
            String projectKey = project.getKey();

            List<String> repositories = settings.getRepositories(projectKey);
            for (String repository : repositories)
            {
                try
                {

                    String username = settings.getUsername(projectKey, repository);
                    String password = settings.getPassword(projectKey, repository);
                    Authentication auth = Authentication.ANONYMOUS;
                    if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password))
                        auth = Authentication.basic(username, password);

                    RepositoryUri uri = RepositoryUri.parse(repository);
                    logger.debug("migrate repository [ {} ]", uri);
                    repositoryPersister.addRepository(projectKey, uri, username, password);

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

							Changeset changeset = BitbucketChangesetFactory.load(bitbucket, uri.getRepositoryUrl(),
									auth, node);
							repositoryPersister.addChangeset(issueId, changeset);
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

    public ModelVersion getModelVersion()
    {
        return ModelVersion.valueOf("1");
    }

}
