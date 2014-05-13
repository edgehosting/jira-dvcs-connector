package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import net.java.ao.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * @author Martin Skurla
 */
// suppressed deprecation - we want to have migrators stable as much as possible
@SuppressWarnings("deprecation")
public class To_10_LastChangesetNodeMigrator implements ActiveObjectsUpgradeTask
{
    private static final Logger log = LoggerFactory.getLogger(To_10_LastChangesetNodeMigrator.class);


    @SuppressWarnings("unchecked")
	@Override
    public void upgrade(ModelVersion currentVersion, ActiveObjects activeObjects)
    {
        log.debug("upgrade [ " + getModelVersion() + " ]");

        activeObjects.migrate(OrganizationMapping.class, RepositoryMapping.class, ChangesetMapping.class);

        populateLastChangesetNodeColumn(activeObjects);
    }

    @Override
    public ModelVersion getModelVersion()
    {
        return ModelVersion.valueOf("10");
    }

    private void populateLastChangesetNodeColumn(ActiveObjects activeObjects)
    {
        for (RepositoryMapping repository : activeObjects.find(RepositoryMapping.class))
        {
            Date repositoryLastCommitDate = repository.getLastCommitDate();

            if (repositoryLastCommitDate != null) // when the repo is empty
            {
				ChangesetMapping[] lastChangesetOrEmptyArray = activeObjects.find(ChangesetMapping.class,
                        Query.select().where(ChangesetMapping.REPOSITORY_ID + " = ?", repository.getID())
                                      .order(ChangesetMapping.DATE + " DESC")
                                      .limit(1));

                // should never happen as empty repo should not have set LAST_COMMIT_DATE
                if (lastChangesetOrEmptyArray.length != 0)
                {
                    repository.setLastChangesetNode(lastChangesetOrEmptyArray[0].getRawNode());
                    repository.save();
                }
                else
                {
                    log.error("Repository '{}' within organization id '{}' has set LAST_COMMIT_DATE, but there are 0 "
                            + "changesets associated with this repository !",
                            repository.getName(), repository.getOrganizationId());
                }
            }
        }
    }
}
