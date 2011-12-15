package com.atlassian.jira.plugins.bitbucket.activeobjects.v2;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

import java.util.Date;

@Table("IssueMappingV2")
public interface IssueMapping extends Entity {

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

    String getMessage();

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
