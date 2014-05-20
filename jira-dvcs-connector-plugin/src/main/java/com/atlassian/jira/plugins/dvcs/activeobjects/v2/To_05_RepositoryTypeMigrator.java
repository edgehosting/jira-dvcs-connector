package com.atlassian.jira.plugins.dvcs.activeobjects.v2;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class To_05_RepositoryTypeMigrator implements ActiveObjectsUpgradeTask
{
    private final Logger logger = LoggerFactory.getLogger(To_05_RepositoryTypeMigrator.class);

    @Override
    @SuppressWarnings("unchecked")
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

    @Override
    public ModelVersion getModelVersion()
    {
        return ModelVersion.valueOf("5");
    }
}
