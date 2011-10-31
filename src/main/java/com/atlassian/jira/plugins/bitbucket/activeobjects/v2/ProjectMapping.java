package com.atlassian.jira.plugins.bitbucket.activeobjects.v2;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

@Table("ProjectMappingV2")
public interface ProjectMapping extends Entity
{
    String getRepositoryTypeId();
    String getRepositoryUrl();
    String getProjectKey();
    String getUsername();
    String getPassword();
    String getAdminPassword();
    String getAdminUsername();

    void setRepositoryTypeId(String repositoryTypeId);
    void setRepositoryUrl(String repositoryUrl);
    void setProjectKey(String projectKey);
    void setUsername(String username);
    void setPassword(String password);
    void setAdminPassword(String pasword);
    void setAdminUsername(String username);
}
