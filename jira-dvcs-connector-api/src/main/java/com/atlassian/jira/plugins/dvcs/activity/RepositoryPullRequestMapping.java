package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

@Table("PULL_REQUEST")
public interface RepositoryPullRequestMapping extends Entity
{
    
    String REMOTE_ID = "REMOTE_ID";
    String TO_REPO_ID = "TO_REPOSITORY_ID";
    String NAME = "NAME";
    String DESCRIPTION = "DESCRIPTION";
    String URL = "URL";
    
    //
    // getters
    //
    Long getRemoteId();
    int getToRepositoryId();
    String getName();
    String getDescription();
    String getUrl();

    //
    // setters
    //
    void setRemoteId(Long id);
    void setToRepoId(int repoId);
    void setPullRequestName(String name);
    void setPullRequestDescription(String description);
    void setPullRequestUrl(String url);
}

