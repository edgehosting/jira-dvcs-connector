package com.atlassian.jira.plugins.bitbucket.activeobjects.v2;

import net.java.ao.Entity;

public interface ProjectMapping2 extends Entity
{
    String getRepositoryUrl();
    String getProjectKey();
    String getUsername();
    String getPassword();

    void setRepositoryUrl(String repositoryId);
    void setProjectKey(String projectKey);
    void setUsername(String username);
    void setPassword(String password);
}
