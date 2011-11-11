package com.atlassian.jira.plugins.bitbucket.activeobjects.v2;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

import java.util.Date;

@Table("ChangesetMappingV2")
public interface ChangesetMapping extends Entity {
    public int getRepositoryId();

    public String getNode();

    public String getRawAuthor();

    public String getAuthor();

    public Date getTimestamp();

    public String getRawNode();

    public String getBranch();

    public String getMessage();

    public String getIssueId();


    public void setRepositoryId(int repositoryId);

    public void setNode(String node);

    public void setRawAuthor(String rawAuthor);

    public void setAuthor(String author);

    public void setTimestamp(Date timestamp);

    public void setRawNode(String rawNode);

    public void setBranch(String branch);

    public void setMessage(String message);

    public void setIssueId(String issueId);

}
