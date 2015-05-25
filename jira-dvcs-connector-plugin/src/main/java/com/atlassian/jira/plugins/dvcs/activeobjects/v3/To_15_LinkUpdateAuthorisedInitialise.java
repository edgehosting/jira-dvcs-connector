package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This upgrade task is to initialise the updateLinkAuthorised column to true (for existing rows) so that a link update
 * will be attempted at least once
 */
public class To_15_LinkUpdateAuthorisedInitialise implements ActiveObjectsUpgradeTask
{
    private static final Logger log = LoggerFactory.getLogger(To_14_NewRepositoryColumn.class);

    @Override
    public ModelVersion getModelVersion()
    {
        return ModelVersion.valueOf("15");
    }

    @Override
    public void upgrade(final ModelVersion currentVersion, final ActiveObjects activeObjects)
    {
        log.info("upgrade [ " + getModelVersion() + " ]");
        activeObjects.migrate(RepositoryMapping.class);
        RepositoryMapping[] repositoryMappings = activeObjects.find(RepositoryMapping.class);
        for (RepositoryMapping repositoryMapping : repositoryMappings)
        {
            initialiseSyncAuthorisation(activeObjects, repositoryMapping);
        }
        log.info("upgrade [ " + getModelVersion() + " ]: finished");
    }

    private void initialiseSyncAuthorisation(ActiveObjects activeObjects, RepositoryMapping repositoryMapping)
    {
        repositoryMapping.setUpdateLinkAuthorised(true);
        repositoryMapping.save();
    }
}
