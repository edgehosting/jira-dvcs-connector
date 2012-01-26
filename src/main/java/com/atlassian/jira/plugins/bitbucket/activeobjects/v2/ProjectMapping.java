package com.atlassian.jira.plugins.bitbucket.activeobjects.v2;

import java.util.Date;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

@Table("ProjectMappingV2")
public interface ProjectMapping extends Entity
{
    public static final String ACCESS_TOKEN = "ACCESS_TOKEN";
    public static final String PASSWORD = "PASSWORD";
    public static final String USERNAME = "USERNAME";
    public static final String ADMIN_PASSWORD = "ADMIN_PASSWORD";
    public static final String ADMIN_USERNAME = "ADMIN_USERNAME";
    public static final String REPOSITORY_TYPE = "REPOSITORY_TYPE";
    public static final String PROJECT_KEY = "PROJECT_KEY";
    public static final String REPOSITORY_URL = "REPOSITORY_URL";
    public static final String REPOSITORY_NAME = "REPOSITORY_NAME";
    
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
