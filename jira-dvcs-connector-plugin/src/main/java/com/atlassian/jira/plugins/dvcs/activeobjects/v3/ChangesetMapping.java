package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import java.util.Date;

import net.java.ao.Entity;
import net.java.ao.ManyToMany;
import net.java.ao.OneToMany;
import net.java.ao.Preload;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

@Preload
@Table("ChangesetMapping")
public interface ChangesetMapping extends Entity
{

    public static final String NODE = "NODE";
    public static final String RAW_AUTHOR = "RAW_AUTHOR";
    public static final String AUTHOR = "AUTHOR";
    public static final String DATE = "DATE";
    public static final String RAW_NODE = "RAW_NODE";
    public static final String BRANCH = "BRANCH";
    public static final String MESSAGE = "MESSAGE";
    public static final String PARENTS_DATA = "PARENTS_DATA";
    public static final String FILES_DATA = "FILES_DATA";
    public static final String VERSION = "VERSION";
    public static final String AUTHOR_EMAIL = "AUTHOR_EMAIL";
    public static final String SMARTCOMMIT_AVAILABLE = "SMARTCOMMIT_AVAILABLE";

    /**
     * Constant used to indicate that parents data could not be saved because they are too many
     */
    public static final String TOO_MANY_PARENTS = "<TOO_MANY_PARENTS>";

    /**
     * Rows at the table can contain data loaded by previous versions of this plugin. Some column data maybe missing because previous
     * versions of plugin was not loading them. To get the updated version of changeset we need to reload the data from the BB/GH servers.
     * This flag marks the row data as latest.
     */
    public static final int LATEST_VERSION = 3;

    @ManyToMany(reverse = "getChangeset", through = "getRepository", value = RepositoryToChangesetMapping.class)
    RepositoryMapping[] getRepositories();

    @OneToMany(reverse = "getChangeset")
    IssueToChangesetMapping[] getIssues();

    String getNode();

    String getRawAuthor();

    String getAuthor();

    Date getDate();

    String getRawNode();

    String getBranch();

    @StringLength(StringLength.UNLIMITED)
    String getMessage();

    @StringLength(StringLength.UNLIMITED)
    String getFilesData();

    String getParentsData();

    Integer getVersion();

    String getAuthorEmail();

    Boolean isSmartcommitAvailable();

    void setNode(String node);

    void setRawAuthor(String rawAuthor);

    void setAuthor(String author);

    void setDate(Date date);

    void setRawNode(String rawNode);

    void setBranch(String branch);

    @StringLength(StringLength.UNLIMITED)
    void setMessage(String message);

    @StringLength(StringLength.UNLIMITED)
    void setFilesData(String files);

    void setParentsData(String parents);

    void setVersion(Integer version);

    void setAuthorEmail(String authorEmail);

    void setSmartcommitAvailable(Boolean available);

    @StringLength(StringLength.UNLIMITED)
    String getFileDetailsJson();
    void setFileDetailsJson(String fileDetailsJson);

    // Deprecated stuff

    /**
     * FIXME: 2.0
     * 
     * @deprecated was removed - kept as temporary state - can be removed in 2.0
     */
    @Deprecated
    public static final String REPOSITORY_ID = "REPOSITORY_ID";

    /**
     * FIXME: 2.0
     * 
     * @deprecated was removed - kept as temporary state - can be removed in 2.0
     */
    @Deprecated
    public static final String PROJECT_KEY = "PROJECT_KEY";
    /**
     * FIXME: 2.0
     * 
     * @deprecated was removed - kept as temporary state - can be removed in 2.0
     */
    @Deprecated
    public static final String ISSUE_KEY = "ISSUE_KEY";

    /**
     * @return {@link #REPOSITORY_ID}
     */
    @Deprecated
    int getRepositoryId();

    /**
     * @param repositoryId
     *            {@link #getRepositoryId()}
     */
    @Deprecated
    void setRepositoryId(int repositoryId);

    /**
     * @return {@link #PROJECT_KEY}
     */
    @Deprecated
    String getProjectKey();

    /**
     * @return {@link #ISSUE_KEY}
     */
    @Deprecated
    String getIssueKey();

    /**
     * @param projectKey
     *            {@link #getProjectKey()}
     */
    @Deprecated
    void setProjectKey(String projectKey);

    /**
     * @param issueKey
     *            {@link #getIssueKey()}
     */
    @Deprecated
    void setIssueKey(String issueKey);

}
