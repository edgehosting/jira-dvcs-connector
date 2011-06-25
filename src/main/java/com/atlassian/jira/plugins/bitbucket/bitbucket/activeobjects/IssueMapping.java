package com.atlassian.jira.plugins.bitbucket.bitbucket.activeobjects;

import net.java.ao.Entity;

/**
 * Active objects storage for the mapping between a bitbucket repository and a jira project.
 */
public interface IssueMapping extends Entity
{
    String getRepositoryOwner();
    String getRepositorySlug();
    String getProjectKey();
    String getNode();
    String getIssueId();

    void setRepositoryOwner(String owner);
    void setRepositorySlug(String slug);
    void setProjectKey(String projectKey);
    void setNode(String node);
    void setIssueId(String issueId);
}
