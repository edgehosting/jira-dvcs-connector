package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

@Table("PullRequest")
public interface RepositoryPullRequestMapping extends Entity
{
    
    String LOCAL_ID = "LOCAL_ID";
    String TO_REPO_ID = "TO_REPO_ID";
    String PULL_REQUEST_NAME = "PULL_REQUEST_NAME";
    String PULL_REQUEST_DESCRIPTION = "PULL_REQUEST_DESCRIPTION";
    String PULL_REQUEST_URL = "PULL_REQUEST_URL";
    String FOUND_ISSUE_KEY = "FOUND_ISSUE_KEY";
    
    //
    // getters
    //
    Integer getLocalId();
    int getToRepoId();
    String getPullRequestName();
    String getPullRequestDescription();
    String getPullRequestUrl();
    boolean isFoundIssueKey();

    //
    // setters
    //
    void setLocalId(Integer id);
    void setToRepoId(int repoId);
    void setPullRequestName(String name);
    void setPullRequestDescription(String description);
    void setPullRequestUrl(String url);
    void setFoundIssueKey(boolean foundIssueKey);
}

