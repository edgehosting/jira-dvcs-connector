package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

import java.util.Date;

@Table("RepositoryMapping")
public interface RepositoryMapping extends Entity
{
    public static final String ORGANIZATION_ID = "ORGANIZATION_ID";
    public static final String NAME = "NAME";
    public static final String HUMAN_NAME = "NAME";
    public static final String LAST_COMMIT_DATE = "LAST_COMMIT_DATE";
    public static final String LINKED = "LINKED";
    public static final String DeleteDelta = "DELETED";

    int getOrganizationId();
    String getName();
    String getHumanName();
    Date getLastCommitDate();
    boolean isLinked();
    boolean isDeleted();

    void setOrganizationId(int organizationId);
    void setName(String name);
    void setHumanName(String humanName);
    void setLastCommitDate(Date lastCommitDate);
    void setLinked(boolean linked);
    void setDeleted(boolean deleted);
}
