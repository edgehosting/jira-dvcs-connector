package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import java.util.Date;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

@Table("SYNC_AUDIT_LOG")
public interface SyncAuditLogMapping extends Entity
{
    String REPO_ID = "REPO_ID";
    String START_DATE = "START_DATE";
    String END_DATE = "END_DATE";
    String SYNC_STATUS = "SYNC_STATUS";
    String EXC_TRACE = "EXC_TRACE";
    String SYNC_TYPE = "SYNC_TYPE"; // hard, soft

    int getRepoId();
    Date getStartDate();
    Date getEndDate();
    String getSyncStatus();
    String getSyncType();
    String getExcTrace();

    void setRepoId(int id);
    void setStartDate(Date date);
    void setEndDate(Date date);
    void setSyncStatus(String status);
    void setSyncType(String type);
    void setExcTrace(String trace);

}
