package com.atlassian.jira.plugins.bitbucket.activeobjects.v2;

import net.java.ao.Entity;

public interface IssueMapping extends Entity
{
    int getRepositoryId();

    String getNode();

    String getIssueId();

    void setRepositoryId(int repositoryId);

    void setNode(String node);

    void setIssueId(String issueId);
}
