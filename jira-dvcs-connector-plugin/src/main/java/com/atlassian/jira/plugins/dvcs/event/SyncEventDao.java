package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.dao.StreamCallback;
import com.atlassian.jira.plugins.dvcs.dao.ao.EntityBeanGenerator;
import com.atlassian.jira.plugins.dvcs.util.ActiveObjectsUtils;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.collect.ImmutableMap;
import net.java.ao.EntityStreamCallback;
import net.java.ao.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.atlassian.jira.plugins.dvcs.event.SyncEventMapping.EVENT_CLASS;
import static com.atlassian.jira.plugins.dvcs.event.SyncEventMapping.EVENT_DATE;
import static com.atlassian.jira.plugins.dvcs.event.SyncEventMapping.EVENT_JSON;
import static com.atlassian.jira.plugins.dvcs.event.SyncEventMapping.REPO_ID;
import static com.atlassian.jira.plugins.dvcs.event.SyncEventMapping.SCHEDULED_SYNC;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * DAO for SyncEventMapping instances.
 */
@Component
public class SyncEventDao
{
    private final ActiveObjects activeObjects;
    private final EntityBeanGenerator beanGenerator;

    @Autowired
    public SyncEventDao(@ComponentImport ActiveObjects activeObjects, EntityBeanGenerator beanGenerator)
    {
        this.activeObjects = checkNotNull(activeObjects);
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
                        EVENT_JSON, syncEventMapping.getEventJson(),
                        SCHEDULED_SYNC, syncEventMapping.getScheduledSync()
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
     * Returns all {code SyncEventMapping}s associated with the given repository id, sorted by date.
     *
     * @param repoId the id of the repository for which to dispatch events
     * @param callback a Callback
     */
    public void streamAllByRepoId(final int repoId, final StreamCallback<SyncEventMapping> callback)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {
            @Override
            public Void doInTransaction()
            {
                Query query = createQueryFor(repoId).order(EVENT_DATE + " ASC");
                query.setWhereParams(new Object[] { repoId });

                activeObjects.stream(SyncEventMapping.class, query, new EntityStreamCallback<SyncEventMapping, Integer>()
                {
                    @Override
                    public void onRowRead(SyncEventMapping syncEventMapping)
                    {
                        callback.callback(activeObjects.get(SyncEventMapping.class, syncEventMapping.getID()));
                    }
                });

                return null;
            }
        });
    }

    /**
     * Deletes all events associated with the given repository id.
     */
    public int deleteAll(final int repoId)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<Integer>()
        {
            @Override
            public Integer doInTransaction()
            {
                return ActiveObjectsUtils.delete(activeObjects, SyncEventMapping.class, createQueryFor(repoId));
            }
        });
    }

    /**
     * @param repoId the id of the Repository
     * @return a new Query to select all events for a repo
     */
    private static Query createQueryFor(final int repoId)
    {
        Query query = Query.select().where(REPO_ID + " = ?");
        query.setWhereParams(new Object[] { repoId });

        return query;
    }
}
