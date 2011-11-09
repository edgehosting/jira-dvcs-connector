package com.atlassian.jira.plugins.bitbucket.activeobjects.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;

public class To_05_RepositoryTypeMigrator implements ActiveObjectsUpgradeTask
{
    private final Logger logger = LoggerFactory.getLogger(To_05_RepositoryTypeMigrator.class);

    public void upgrade(ModelVersion currentVersion, ActiveObjects activeObjects)
    {
        logger.debug("upgrade [ " + currentVersion + " ]");
        
        activeObjects.migrate(ProjectMapping.class, IssueMapping.class);
        
        ProjectMapping[] projectMappings = activeObjects.find(ProjectMapping.class);
        for (ProjectMapping projectMapping : projectMappings)
        {
            projectMapping.setRepositoryType("bitbucket");
            projectMapping.save();
        }
    }

    public ModelVersion getModelVersion()
    {
        return ModelVersion.valueOf("5");
    }
}
