package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import net.java.ao.Entity;
import net.java.ao.OneToMany;
import net.java.ao.Preload;
import net.java.ao.schema.Default;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

import java.util.Date;

@Preload
@Table ("RepositoryMapping")
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
    public static final String LOGO = "LOGO";
    public static final String IS_FORK = "FORK";
    public static final String FORK_OF_SLUG = "FORK_OF_SLUG";
    public static final String FORK_OF_NAME = "FORK_OF_NAME";
    public static final String FORK_OF_OWNER = "FORK_OF_OWNER";
    public static final String UPDATE_LINK_AUTHORISED = "UPDATE_LINK_AUTHORISED";


    @OneToMany (reverse = "getRepository")
    RepositoryToProjectMapping[] getLinkedProjects();

    @Indexed
    int getOrganizationId();

    String getSlug();

    String getName();

    Date getLastCommitDate();

    @Deprecated
    String getLastChangesetNode();

    @Indexed
    boolean isLinked();

    boolean isDeleted();

    boolean isSmartcommitsEnabled();

    Date getActivityLastSync();

    @StringLength (StringLength.UNLIMITED)
    String getLogo();

    boolean isFork();

    String getForkOfName();

    String getForkOfSlug();

    String getForkOfOwner();

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

    @StringLength (StringLength.UNLIMITED)
    void setLogo(String logo);

    void setFork(boolean isFork);

    void setForkOfSlug(String slug);

    void setForkOfName(String name);

    void setForkOfOwner(String owner);

    void setUpdateLinkAuthorised(boolean authorised);

    boolean getUpdateLinkAuthorised();
}
