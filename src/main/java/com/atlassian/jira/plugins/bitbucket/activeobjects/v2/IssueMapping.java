package com.atlassian.jira.plugins.bitbucket.activeobjects.v2;

import net.java.ao.Entity;
import net.java.ao.schema.SQLType;
import net.java.ao.schema.Table;

import java.sql.Types;
import java.util.Date;

@Table("IssueMappingV2")
public interface IssueMapping extends Entity {

    public static final String REPOSITORY_ID = "REPOSITORY_ID";
    public static final String ISSUE_ID = "ISSUE_ID";
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
    /**
     * Rows at the table can contain data loaded by previous versions of this plugin. Some column data maybe missing 
     * because previous versions of plugin was not loading them. To get the updated version of changeset we need 
     * to reload the data from the BB/GH servers. This flag marks the row data as latest.
     */
    public static final int LATEST_VERSION = 2;

    int getRepositoryId();

    String getNode();

    String getIssueId();

    String getRawAuthor();

    String getAuthor();

    Date getDate();

    String getRawNode();

    String getBranch();

    @SQLType(Types.CLOB)
    String getMessage();

    @SQLType(Types.CLOB)
    String getFilesData();

    String getParentsData();

    Integer getVersion();


    void setRepositoryId(int repositoryId);

    void setNode(String node);

    void setIssueId(String issueId);

    void setRawAuthor(String rawAuthor);

    void setAuthor(String author);

    void setDate(Date date);

    void setRawNode(String rawNode);

    void setBranch(String branch);

    void setMessage(String message);

    void setFilesData(String files);

    void setParentsData(String parents);

    void setVersion(Integer version);
}
