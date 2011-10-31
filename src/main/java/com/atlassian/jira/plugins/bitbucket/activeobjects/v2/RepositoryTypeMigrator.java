package com.atlassian.jira.plugins.bitbucket.activeobjects.v2;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryTypeMigrator implements ActiveObjectsUpgradeTask {

    private final Logger logger = LoggerFactory.getLogger(ActiveObjectsV2Migrator.class);

    public void upgrade(ModelVersion currentVersion, ActiveObjects activeObjects) {
        logger.debug("upgrade [ " + currentVersion + " ]");

        ProjectMapping[] projectMappings = activeObjects.find(ProjectMapping.class);
        for (ProjectMapping projectMapping : projectMappings) {
            projectMapping.setRepositoryTypeId("bitbucket");
            projectMapping.save();
        }
    }

    public ModelVersion getModelVersion() {
        return ModelVersion.valueOf("6");
    }
}
