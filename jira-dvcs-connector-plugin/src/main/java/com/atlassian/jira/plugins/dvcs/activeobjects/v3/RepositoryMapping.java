package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Table;

import java.util.Date;

@Preload
@Table("RepositoryMapping")
public interface RepositoryMapping extends Entity
{
    public static final String ORGANIZATION_ID = "ORGANIZATION_ID";
    public static final String SLUG = "SLUG";
    public static final String NAME = "NAME";
    public static final String LAST_COMMIT_DATE = "LAST_COMMIT_DATE";//TODO could be removed?
    public static final String LAST_CHANGESET_NODE = "LAST_CHANGESET_NODE";
    public static final String LINKED = "LINKED";
    public static final String DELETED = "DELETED";
    public static final String SMARTCOMMITS_ENABLED = "SMARTCOMMITS_ENABLED";

    int getOrganizationId();
    String getSlug();
    String getName();
    Date getLastCommitDate();
    String getLastChangesetNode();
    boolean isLinked();
    boolean isDeleted();
    boolean isSmartcommitsEnabled();

    void setOrganizationId(int organizationId);
    void setSlug(String slug);
    void setName(String name);
    void setLastCommitDate(Date lastCommitDate);
    void setLastChangesetNode(String lastChangesetNode);
    void setLinked(boolean linked);
    void setDeleted(boolean deleted);
    void setSmartcommitsEnabled(boolean enabled);
}
