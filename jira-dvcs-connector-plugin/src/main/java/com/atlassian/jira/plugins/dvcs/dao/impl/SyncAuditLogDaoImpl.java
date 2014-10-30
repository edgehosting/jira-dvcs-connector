package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.SyncAuditLogMapping;
import com.atlassian.jira.plugins.dvcs.analytics.DvcsSyncEndAnalyticsEvent;
import com.atlassian.jira.plugins.dvcs.dao.SyncAuditLogDao;
import com.atlassian.jira.plugins.dvcs.util.ActiveObjectsUtils;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.transaction.TransactionCallback;
import net.java.ao.Query;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public class SyncAuditLogDaoImpl implements SyncAuditLogDao
{
    private static final int BIG_DATA_PAGESIZE = 200;
    private static final int ROTATION_PERIOD = 1000 * 60 * 60 * 24 * 7;

    private final ActiveObjects ao;

    private static final Logger log = LoggerFactory.getLogger(SyncAuditLogDaoImpl.class);
    private EventPublisher eventPublisher;

    @Autowired
    public SyncAuditLogDaoImpl(@ComponentImport ActiveObjects ao, @ComponentImport EventPublisher publisher)
    {
        super();
        this.ao = checkNotNull(ao);
        this.eventPublisher = checkNotNull(publisher);
    }

    @Override
    public SyncAuditLogMapping newSyncAuditLog(final int repoId, final String syncType, final Date startDate)
    {
        return doTxQuietly(new Callable<SyncAuditLogMapping>(){
            @Override
            public SyncAuditLogMapping call() throws Exception
            {
                //
                rotate(repoId);
                //
                Map<String, Object> data = new HashMap<String, Object>();
                data.put(SyncAuditLogMapping.REPO_ID, repoId);
                data.put(SyncAuditLogMapping.SYNC_TYPE, syncType);
                data.put(SyncAuditLogMapping.START_DATE, startDate);
                data.put(SyncAuditLogMapping.SYNC_STATUS, SyncAuditLogMapping.SYNC_STATUS_RUNNING);
                data.put(SyncAuditLogMapping.TOTAL_ERRORS , 0);
                return ao.create(SyncAuditLogMapping.class, data);
            }

            private void rotate(int repoId) 
            {
                ActiveObjectsUtils.delete(ao, SyncAuditLogMapping.class,
                        Query.select().from(SyncAuditLogMapping.class).where(SyncAuditLogMapping.REPO_ID + " = ? AND " + SyncAuditLogMapping.START_DATE + " < ?" , repoId, new Date(System.currentTimeMillis() - ROTATION_PERIOD))
                        );
            }
        });
    }

    @Override
    public SyncAuditLogMapping finish(final int syncId, final Date firstRequestDate, final int numRequests, final int flightTimeMs, final Date finishDate)
    {
        return doTxQuietly(new Callable<SyncAuditLogMapping>(){
            @Override
            public SyncAuditLogMapping call() throws Exception
            {
                SyncAuditLogMapping mapping = find(syncId);
                if (mapping != null)
                {
                    mapping.setFirstRequestDate(firstRequestDate);
                    mapping.setEndDate(finishDate);
                    mapping.setNumRequests(numRequests);
                    mapping.setFlightTimeMs(flightTimeMs);

                    if (StringUtils.isNotBlank(mapping.getExcTrace()))
                    {
                        mapping.setSyncStatus(SyncAuditLogMapping.SYNC_STATUS_FAILED);
                    } else
                    {
                        mapping.setSyncStatus(SyncAuditLogMapping.SYNC_STATUS_SUCCESS);
                    }

                    mapping.save();
                    
                    fireAnalyticsEvent(mapping);
                }
                return mapping;
            }

        });
    }
    
    private void fireAnalyticsEvent(SyncAuditLogMapping sync)
    {
        String syncTypeString = sync.getSyncType() == null ? "" : sync.getSyncType();

        boolean soft = syncTypeString.contains(SyncAuditLogMapping.SYNC_TYPE_SOFT);
        boolean commits = syncTypeString.contains(SyncAuditLogMapping.SYNC_TYPE_CHANGESETS);
        boolean pullRequests = syncTypeString.contains(SyncAuditLogMapping.SYNC_TYPE_PULLREQUESTS);
        boolean webhook = syncTypeString.contains(SyncAuditLogMapping.SYNC_TYPE_WEBHOOKS);

        eventPublisher.publish(new DvcsSyncEndAnalyticsEvent(soft, commits, pullRequests, webhook, sync.getEndDate(), sync.getStartDate()
                .getTime() - sync.getEndDate().getTime()));
    }

    @Override
    public SyncAuditLogMapping pause(final int syncId)
    {
        return status(syncId);
    }

    protected SyncAuditLogMapping status(final int syncId)
    {
        return doTxQuietly(new Callable<SyncAuditLogMapping>(){
            @Override
            public SyncAuditLogMapping call() throws Exception
            {
                SyncAuditLogMapping mapping = find(syncId);
                if (mapping != null)
                {
                    mapping.setSyncStatus(SyncAuditLogMapping.SYNC_STATUS_SLEEPING);
                    mapping.save();
                }
                return mapping;
            }
        });
    }

    @Override
    public SyncAuditLogMapping resume(final int syncId)
    {
        return doTxQuietly(new Callable<SyncAuditLogMapping>(){
            @Override
            public SyncAuditLogMapping call() throws Exception
            {
                SyncAuditLogMapping mapping = find(syncId);
                if (mapping != null)
                {
                    if (SyncAuditLogMapping.SYNC_STATUS_SLEEPING.equals(mapping.getSyncStatus()))
                    {
                        mapping.setSyncStatus(SyncAuditLogMapping.SYNC_STATUS_RUNNING);
                        mapping.save();
                    }
                }
                return mapping;
            }
        });
    }

    @Override
    public int removeAllForRepo(final int repoId)
    {
        Integer ret = doTxQuietly(new Callable<Integer>(){
            @Override
            public Integer call() throws Exception
            {
                return ActiveObjectsUtils.delete(ao, SyncAuditLogMapping.class, repoQuery(repoId).q());
            }
        });
        return ret == null ? -1 : ret;
    }

    @Override
    public SyncAuditLogMapping setException(final int syncId, final Throwable t, final boolean overwriteOld)
    {
        return doTxQuietly(new Callable<SyncAuditLogMapping>(){
            @Override
            public SyncAuditLogMapping call() throws Exception
            {
                SyncAuditLogMapping found = find(syncId);
                boolean noExceptionYet = StringUtils.isBlank(found.getExcTrace());

                if (t != null && (overwriteOld || noExceptionYet))
                {
                    found.setExcTrace(ExceptionUtils.getStackTrace(t));
                }

                found.setTotalErrors(found.getTotalErrors() + 1);
                found.save();

                return found;
            }
        });
    }

    @Override
    public SyncAuditLogMapping[] getAllForRepo(final int repoId, final Integer page)
    {
        return doTxQuietly(new Callable<SyncAuditLogMapping []>(){
            @Override
            public SyncAuditLogMapping [] call() throws Exception
            {
                return ao.find(SyncAuditLogMapping.class, repoQuery(repoId).page(page).order(SyncAuditLogMapping.START_DATE + " DESC"));
            }
        });
    }

    @Override
    public SyncAuditLogMapping[] getAll(final Integer page)
    {
        return doTxQuietly(new Callable<SyncAuditLogMapping []>(){
            @Override
            public SyncAuditLogMapping [] call() throws Exception
            {
                return ao.find(SyncAuditLogMapping.class, pageQuery(Query.select().order(SyncAuditLogMapping.START_DATE + " DESC"), page));
            }
        });
    }

    @Override
    public SyncAuditLogMapping getLastForRepo(final int repoId)
    {
        return doTxQuietly(new Callable<SyncAuditLogMapping>(){
            @Override
            public SyncAuditLogMapping call() throws Exception
            {
                SyncAuditLogMapping[] found = ao.find(SyncAuditLogMapping.class,
                        repoQuery(repoId).q().limit(1).order(SyncAuditLogMapping.START_DATE + " DESC"));
                return found.length == 1 ? found[0] : null;
            }
        });
    }

    @Override
    public SyncAuditLogMapping getLastSuccessForRepo(final int repoId)
    {
        return doTxQuietly(new Callable<SyncAuditLogMapping>(){
            @Override
            public SyncAuditLogMapping call() throws Exception
            {
                Query query = statusQueryLimitOne(repoId, SyncAuditLogMapping.SYNC_STATUS_SUCCESS);
                SyncAuditLogMapping[] found = ao.find(SyncAuditLogMapping.class, query);
                return found.length == 1 ? found[0] : null;
            }
        });
    }

    @Override
    public SyncAuditLogMapping getLastFailedForRepo(final int repoId)
    {
        return doTxQuietly(new Callable<SyncAuditLogMapping>(){
            @Override
            public SyncAuditLogMapping call() throws Exception
            {
                Query query = statusQueryLimitOne(repoId, SyncAuditLogMapping.SYNC_STATUS_FAILED);
                SyncAuditLogMapping[] found = ao.find(SyncAuditLogMapping.class, query);
                return found.length == 1 ? found[0] : null;
            }
        });
    }

    @Override
    public boolean hasException(final int syncId)
    {
        Boolean ret = doTxQuietly(new Callable<Boolean>(){
            @Override
            public Boolean call() throws Exception
            {
                SyncAuditLogMapping found = find(syncId);
                return found != null && StringUtils.isNotBlank(found.getExcTrace());
            }
        });
        return ret == null ? false : ret;
    }

    private SyncAuditLogMapping find(int syncId)
    {
        return ao.get(SyncAuditLogMapping.class, syncId);
    }

    private PageableQuery repoQuery(final int repoId)
    {
        return new PageableQuery(repoId);
    }

    private Query statusQueryLimitOne(int repoId, String status)
    {
        return Query.select()
            .from(SyncAuditLogMapping.class)
            .where(SyncAuditLogMapping.REPO_ID + " = ? AND " + SyncAuditLogMapping.SYNC_STATUS + " = ?", repoId, status)
            .limit(1)
            .order(SyncAuditLogMapping.START_DATE + " DESC");
    }

    private <RET> RET doTxQuietly(final Callable<RET> callable) {
        return
        ao.executeInTransaction(new TransactionCallback<RET>()
        {
            @Override
            public RET doInTransaction()
            {
                try
                {
                    return callable.call();
                } catch (Throwable e)
                {
                    log.warn("Problem during sync audit log. " + e.getMessage());
                    if (log.isDebugEnabled())
                    {
                        log.debug("Sync audit log.", e);
                    }
                    return null;
                }
            }
        });
    }

    class PageableQuery {
        private Query q;
        private PageableQuery(int repoId)
        {
            super();
            this.q = Query.select().from(SyncAuditLogMapping.class).where(SyncAuditLogMapping.REPO_ID + " = ?", repoId);
        }
        PageableQuery offset(int offset)
        {
            q.setOffset(offset);
            return this;
        }
        PageableQuery limit (int limit) {
            q.setLimit(limit);
            return this;
        }
        Query page(Integer page) {
            pageQuery(q, page);
            return q;
        }
        Query q() {
            return q;
        }
    }

    private static Query pageQuery(Query q, Integer page)
    {
        q.setLimit(BIG_DATA_PAGESIZE);
        if (page == null)
        {
            q.setOffset(0);
        } else {
            q.setOffset(BIG_DATA_PAGESIZE * page);
        }
        return q;
    }
}
