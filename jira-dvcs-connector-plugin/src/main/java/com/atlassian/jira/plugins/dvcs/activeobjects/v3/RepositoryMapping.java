package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import java.util.Date;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Table;

@Preload
@Table("RepositoryMapping")
public interface RepositoryMapping extends Entity
{
    public static final String ORGANIZATION_ID = "ORGANIZATION_ID";
    public static final String SLUG = "SLUG";
    public static final String NAME = "NAME";
    public static final String LAST_COMMIT_DATE = "LAST_COMMIT_DATE";
    public static final String LINKED = "LINKED";
    public static final String DELETED = "DELETED";
    public static final String SMARTCOMMITS_ENABLED = "SMARTCOMMITS_ENABLED";
    public static final String ACTIVITY_LAST_SYNC = "ACTIVITY_LAST_SYNC";

    int getOrganizationId();
    String getSlug();
    String getName();
    Date getLastCommitDate();
    @Deprecated
    String getLastChangesetNode();
    boolean isLinked();
    boolean isDeleted();
    boolean isSmartcommitsEnabled();
    Date getActivityLastSync();

    void setOrganizationId(int organizationId);
    void setSlug(String slug);
    void setName(String name);
    void setLastCommitDate(Date lastCommitDate);
    @Deprecated
    void setLastChangesetNode(String lastChangesetNode);
    void setLinked(boolean linked);
    void setDeleted(boolean deleted);
    void setSmartcommitsEnabled(boolean enabled);
    void setActivityLastSync(Date dateOrNull);
}
