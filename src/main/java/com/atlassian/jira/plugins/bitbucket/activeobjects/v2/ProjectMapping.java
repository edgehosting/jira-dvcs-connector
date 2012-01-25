package com.atlassian.jira.plugins.bitbucket.activeobjects.v2;

import java.util.Date;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

@Table("ProjectMappingV2")
public interface ProjectMapping extends Entity
{
    String getRepositoryName();
    String getRepositoryType();
    String getRepositoryUrl();
    String getProjectKey();
    String getUsername();
    String getPassword();
    String getAdminPassword();
    String getAdminUsername();
    String getAccessToken();
    Date getLastCommitDate();

    void setRepositoryName(String repositoryName);
    void setRepositoryType(String repositoryType);
    void setRepositoryUrl(String repositoryUrl);
    void setProjectKey(String projectKey);
    void setUsername(String username);
    void setPassword(String password);
    void setAdminPassword(String pasword);
    void setAdminUsername(String username);
    void setAccessToken(String accessToken);
    void setLastCommitDate(Date lastSyncDate);

}
