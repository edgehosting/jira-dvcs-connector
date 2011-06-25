package com.atlassian.jira.plugins.bitbucket.bitbucket.activeobjects;

import net.java.ao.Entity;

/**
 * Active objects storage for the mapping between a bitbucket repository and a jira project.
 */
public interface ProjectMapping extends Entity
{
    String getRepositoryOwner();
    String getRepositorySlug();
    String getProjectKey();
    String getUsername();
    String getPassword();

    void setRepositoryOwner(String owner);
    void setRepositorySlug(String slug);
    void setProjectKey(String projectKey);
    void setUsername(String username);
    void setPassword(String password);
}
