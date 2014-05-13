package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class To_09_SmartCommitsColumnsMigrator implements ActiveObjectsUpgradeTask
{
    private static final Logger log = LoggerFactory.getLogger(To_09_SmartCommitsColumnsMigrator.class);

    @SuppressWarnings("unchecked")
    @Override
    public void upgrade(ModelVersion currentVersion, ActiveObjects activeObjects)
    {
        log.debug("upgrade [ " + getModelVersion() + " ]");

        activeObjects.migrate(OrganizationMapping.class, RepositoryMapping.class, ChangesetMapping.class);

    }

    @Override
    public ModelVersion getModelVersion()
    {
        return ModelVersion.valueOf("9");
    }
}
