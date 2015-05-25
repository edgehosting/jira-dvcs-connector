package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Upgrade task was removed from the upgrade tasks in the atlassian-plugin.xml to fix https://jdog.jira-dev.com/browse/FUSE-466
 */
public class To_14_NewRepositoryColumn implements ActiveObjectsUpgradeTask
{
    private static final Logger log = LoggerFactory.getLogger(To_14_NewRepositoryColumn.class);

    @Override
    public void upgrade(ModelVersion currentVersion, ActiveObjects activeObjects)
    {
        log.info("upgrade [ " + getModelVersion() + " ]");

        activeObjects.migrate(OrganizationMapping.class, RepositoryMapping.class, ChangesetMapping.class,
                IssueToChangesetMapping.class, RepositoryToChangesetMapping.class, BranchHeadMapping.class);

        log.info("upgrade [ " + getModelVersion() + " ]: finished");
    }

    @Override
    public ModelVersion getModelVersion()
    {
        return ModelVersion.valueOf("14");
    }

}
