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
import com.atlassian.jira.plugins.bitbucket.api.Authentication;
import com.atlassian.jira.plugins.bitbucket.api.Encryptor;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryPersister;
import com.atlassian.jira.plugins.bitbucket.api.impl.DefaultRepositoryPersister;
import com.atlassian.jira.plugins.bitbucket.api.impl.DefaultSourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.common.Changeset;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketChangesetFactory;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketCommunicator;
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
    private final BitbucketCommunicator bitbucket;

	private final Encryptor encryptor;

    public PropertyMigrator(ProjectManager projectManager, BitbucketProjectSettings settings,
                            BitbucketCommunicator bitbucket, Encryptor encryptor)
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
    	RepositoryPersister repositoryPersister = new DefaultRepositoryPersister(activeObjects);
    	
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
                    ProjectMapping pm = repositoryPersister.addRepository(projectKey, uri, username, password);

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
            }
        }
        logger.debug("completed property migration");
    }

    public ModelVersion getModelVersion()
    {
        return ModelVersion.valueOf("1");
    }

}
