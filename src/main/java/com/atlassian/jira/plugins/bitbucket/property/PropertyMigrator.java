package com.atlassian.jira.plugins.bitbucket.property;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.jira.plugins.bitbucket.bitbucket.*;
import com.atlassian.jira.plugins.bitbucket.bitbucket.activeobjects.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.bitbucket.activeobjects.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.bitbucket.impl.DefaultBitbucketMapper;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.sal.api.transaction.TransactionCallback;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

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

        BitbucketMapper mapper = new DefaultBitbucketMapper(activeObjects, bitbucket, encryptor);

        List<Project> projects = projectManager.getProjectObjects();
        for (Project project : projects)
        {
            String projectKey = project.getKey();

            List<String> repositories = settings.getRepositories(projectKey);
            for (String repository : repositories)
            {
                try
                {
                    URL repositoryURL = new URL(repository);

                    String username = settings.getUsername(projectKey, repository);
                    String password = settings.getPassword(projectKey, repository);
                    BitbucketAuthentication auth = BitbucketAuthentication.ANONYMOUS;
                    if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password))
                        auth = BitbucketAuthentication.basic(username, password);

                    String[] path = repositoryURL.getPath().split("/");
                    String owner = path[1];
                    String slug = path[2];
                    BitbucketRepository bitbucketRepository = BitbucketRepositoryFactory.load(bitbucket, auth, owner, slug);

                    logger.debug("owner [ " + owner + " ] slug [ " + slug + " ]");

                    List<String> issueIds = settings.getIssueIds(projectKey, repository);
                    for (String issueId : issueIds)
                    {

                        List<String> commits = settings.getCommits(projectKey, repository, issueId);

                        for (String commit : commits)
                        {
                            URL changesetURL = new URL(commit);
                            String changesetPath = changesetURL.getPath();
                            String node = changesetPath.substring(changesetPath.lastIndexOf("/") + 1);
                            logger.debug("add changeset [ " + issueId + " ] [ " + node + " ]");

                            mapper.addChangeset(issueId, BitbucketChangesetFactory.load(bitbucket, auth, owner, slug, node));
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
