package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.collect.Lists;
import net.java.ao.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class To_13_RemoveFutureChangesets implements ActiveObjectsUpgradeTask
{
    private static final Logger log = LoggerFactory.getLogger(To_13_RemoveFutureChangesets.class);
    private static final Date DATE_IN_THE_PAST = new Date(0); // January 1, 1970, 00:00:00 GMT
    private static final Date TOMORROW_DATE = getTomorrow(); // we will adjust all dates after tomorrow

    private static Date getTomorrow()
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DATE, 1);
        return cal.getTime();
    }

    @Override
    public void upgrade(ModelVersion currentVersion, ActiveObjects activeObjects)
    {
        log.info("upgrade [ " + getModelVersion() + " ]: started");

        try
        {
            activeObjects.migrate(OrganizationMapping.class, RepositoryMapping.class, ChangesetMapping.class,
                    IssueToChangesetMapping.class, RepositoryToChangesetMapping.class, BranchHeadMapping.class);
        
            for (ChangesetMapping changesetMapping : getChangesetsFromFuture(activeObjects))
            {
                setChangesetDate(activeObjects, DATE_IN_THE_PAST, changesetMapping);
            }

            log.info("upgrade [ " + getModelVersion() + " ]: finished");
        } catch(RuntimeException e)
        {
            log.warn("Cleaning of future dates did not finished correctly. This will not affect the behavior, only the changesets from the future will be still there. To fix them run full synchronization.", e);
        }
    }

    private void setChangesetDate(ActiveObjects activeObjects, Date dateInThePast, final ChangesetMapping changesetMapping)
    {
        activeObjects.executeInTransaction(new TransactionCallback<ChangesetMapping>()
        {
            @Override
            public ChangesetMapping doInTransaction()
            {
                log.warn("Changeset [{}] has date set in the future [{}]. Setting date to [{}].", new Object[] {
                        changesetMapping.getNode(), changesetMapping.getDate(), DATE_IN_THE_PAST });
                changesetMapping.setDate(DATE_IN_THE_PAST);
                changesetMapping.setSmartcommitAvailable(false); // ignore smart commits
                changesetMapping.save();
                return changesetMapping;
            }
        });
    }

    private Iterable<ChangesetMapping> getChangesetsFromFuture(final ActiveObjects activeObjects)
    {
        return new Iterable<ChangesetMapping>()
        {
            @Override
            public Iterator<ChangesetMapping> iterator()
            {
                return new ChangesetsFromFutureIterator(activeObjects);
            }
        };
    }

    @Override
    public ModelVersion getModelVersion()
    {
        return ModelVersion.valueOf("13");
    }

    // --------------------------------------------------------------------------------
    // ----------------------  ChangesetsFromFutureIterator  --------------------------
    // --------------------------------------------------------------------------------
    /**
     * Returns changesets where date is greater than tomorrow
     */
    private final class ChangesetsFromFutureIterator implements Iterator<ChangesetMapping>
    {
        private final ActiveObjects activeObjects;
        private final List<ChangesetMapping> currentPage = Lists.newArrayList();

        public ChangesetsFromFutureIterator(ActiveObjects activeObjects)
        {
            this.activeObjects = activeObjects;
        }

        @Override
        public boolean hasNext()
        {
            readCurrentPage();
            return !currentPage.isEmpty();
        }

        @Override
        public ChangesetMapping next()
        {
            if (!hasNext())
            {
                throw new NoSuchElementException();
            }
            return currentPage.remove(0);
        }


        private void readCurrentPage()
        {
            if (!currentPage.isEmpty())
            {
                return;
            }
            // select 20 newest changesets
            Query query = Query.select().order(ChangesetMapping.DATE + " desc");
            query.limit(20);
            ChangesetMapping[] latestChangesets = activeObjects.find(ChangesetMapping.class, query);
            // filter in those from the future
            for (ChangesetMapping changesetMapping : latestChangesets)
            {
                if (changesetMapping.getDate() != null && changesetMapping.getDate().after(TOMORROW_DATE))
                {
                    currentPage.add(changesetMapping);
                }
            }
        }
        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

}
