package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import net.java.ao.schema.Table;

import java.util.Date;

@Table("RepositoryMapping")
public interface RepositoryMapping
{
    public static final String ORGANIZATION_ID = "ORGANIZATION_ID";
    public static final String NAME = "NAME";
    public static final String LAST_COMMIT_DATE = "LAST_COMMIT_DATE";
    public static final String LINKED = "LINKED";

    int getOrganizationId();
    String getName();
    Date getLastCommitDate();
    boolean isLinked();

    void setOrganizationId(int organizationId);
    void setName(String name);
    void setLastCommitDate(Date lastCommitDate);
    void setLinked(boolean linked);
}
