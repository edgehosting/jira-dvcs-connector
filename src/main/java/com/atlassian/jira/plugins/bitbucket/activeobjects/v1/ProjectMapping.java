package com.atlassian.jira.plugins.bitbucket.activeobjects.v1;

import net.java.ao.Entity;

/**
 * Active objects storage for the mapping between a bitbucket repository and a jira project.
 */
public interface ProjectMapping extends Entity
{
    String getRepositoryUri();
    String getProjectKey();
    String getUsername();
    String getPassword();

    void setRepositoryUri(String owner);
    void setProjectKey(String projectKey);
    void setUsername(String username);
    void setPassword(String password);
}
