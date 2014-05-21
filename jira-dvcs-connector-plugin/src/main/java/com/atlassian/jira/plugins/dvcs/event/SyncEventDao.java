package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.dao.ao.EntityBeanGenerator;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.java.ao.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.atlassian.jira.plugins.dvcs.event.SyncEventMapping.EVENT_CLASS;
import static com.atlassian.jira.plugins.dvcs.event.SyncEventMapping.EVENT_DATE;
import static com.atlassian.jira.plugins.dvcs.event.SyncEventMapping.EVENT_JSON;
import static com.atlassian.jira.plugins.dvcs.event.SyncEventMapping.REPO_ID;

/**
 * DAO for SyncEventMapping instances.
 */
@Component
public class SyncEventDao
{
    private final ActiveObjects activeObjects;
    private final EntityBeanGenerator beanGenerator;

    @Autowired
    public SyncEventDao(ActiveObjects activeObjects, EntityBeanGenerator beanGenerator)
    {
        this.activeObjects = activeObjects;
        this.beanGenerator = beanGenerator;
    }

    /**
     * Returns a new (not yet persisted) SyncEventMapping instance that can be saved.
     *
     * @return a SyncEventMapping
     */
    public SyncEventMapping create()
    {
        return beanGenerator.createInstanceOf(SyncEventMapping.class);
    }

    /**
     * Saves the given SyncEventMapping.
     *
     * @param syncEventMapping a SyncEventMapping
     */
    public SyncEventMapping save(final SyncEventMapping syncEventMapping)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<SyncEventMapping>()
        {
            @Override
            public SyncEventMapping doInTransaction()
            {
                return activeObjects.create(SyncEventMapping.class, ImmutableMap.<String, Object>of(
                        REPO_ID, syncEventMapping.getRepoId(),
                        EVENT_DATE, syncEventMapping.getEventDate(),
                        EVENT_CLASS, syncEventMapping.getEventClass(),
                        EVENT_JSON, syncEventMapping.getEventJson()
                ));
            }
        });
    }

    /**
     * Deletes the given SyncEventMapping.
     *
     * @param syncEventMapping a SyncEventMapping
     */
    public void delete(final SyncEventMapping syncEventMapping)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {
            @Override
            public Void doInTransaction()
            {
                activeObjects.delete(syncEventMapping);
                return null;
            }
        });
    }

    /**
     * Returns all {code SyncEvent}s associated with the given repository id, sorted by date.
     *
     * @param repoId the id of the repository for which to dispatch events
     */
    public List<SyncEventMapping> findAllByRepoId(final int repoId)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<ImmutableList<SyncEventMapping>>()
        {
            @Override
            public ImmutableList<SyncEventMapping> doInTransaction()
            {
                Query query = Query.select().where(REPO_ID + " = ?").order(EVENT_DATE + " ASC");
                query.setWhereParams(new Object[] { repoId });

                return ImmutableList.copyOf(activeObjects.find(SyncEventMapping.class, query));
            }
        });
    }
}
