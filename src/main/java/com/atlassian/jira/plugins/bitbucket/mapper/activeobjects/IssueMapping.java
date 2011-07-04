package com.atlassian.jira.plugins.bitbucket.mapper.activeobjects;

import net.java.ao.Entity;

/**
 * Active objects storage for the mapping between a bitbucket repository and a jira project.
 */
public interface IssueMapping extends Entity
{
    String getRepositoryUri();

    String getProjectKey();

    String getNode();

    String getIssueId();

    void setRepositoryUri(String owner);

    void setProjectKey(String projectKey);

    void setNode(String node);

    void setIssueId(String issueId);
}
