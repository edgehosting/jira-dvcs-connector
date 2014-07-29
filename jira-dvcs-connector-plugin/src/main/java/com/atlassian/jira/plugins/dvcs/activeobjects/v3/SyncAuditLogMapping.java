package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

import java.util.Date;

@Preload
@Table("SyncAuditLog")
public interface SyncAuditLogMapping extends Entity
{
    String SYNC_STATUS_RUNNING = "RUNNING";
    String SYNC_STATUS_FAILED = "FAILED";
    String SYNC_STATUS_SUCCESS = "SUCCESS";
    String SYNC_STATUS_SLEEPING = "SLEEPING";
    String SYNC_TYPE_SOFT = "SOFT";
    String SYNC_TYPE_CHANGESETS = "CHANGESETS";
    String SYNC_TYPE_PULLREQUESTS = "PULLREQUESTS";
    String SYNC_TYPE_WEBHOOKS = "WEBHOOKS";

    //
    String REPO_ID = "REPO_ID";
    String START_DATE = "START_DATE";
    String END_DATE = "END_DATE";
    String SYNC_STATUS = "SYNC_STATUS";
    String EXC_TRACE = "EXC_TRACE";
    String SYNC_TYPE = "SYNC_TYPE"; // hard, soft
    String TOTAL_ERRORS  = "TOTAL_ERRORS";

    @Indexed
    int getRepoId();
    Date getStartDate();
    Date getEndDate();
    String getSyncStatus();
    int getFlightTimeMs();
    Date getFirstRequestDate();
    int getNumRequests();
    String getSyncType();
    @StringLength(StringLength.UNLIMITED)
    String getExcTrace();
    int getTotalErrors();

    void setRepoId(int id);
    void setStartDate(Date date);
    void setEndDate(Date date);
    void setFirstRequestDate(Date firstRequestDate);
    void setSyncStatus(String status);
    void setFlightTimeMs(int flightTime);
    void setNumRequests(int numRequests);
    void setSyncType(String type);
    @StringLength(StringLength.UNLIMITED)
    void setExcTrace(String trace);
    void setTotalErrors(int total);

}
