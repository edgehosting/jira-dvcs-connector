package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

@Table("PullRequest")
public interface RepositoryPullRequestMapping extends Entity {
    
    String LOCAL_ID = "LOCAL_ID";
    String TO_REPO_SLUG = "TO_REPO_SLUG";
    String PULL_REQUEST_NAME = "PULL_REQUEST_NAME";
    String PULL_REQUEST_URL = "PULL_REQUEST_URL";
    String FOUND_ISSUE_KEY = "FOUND_ISSUE_KEY";
    
    //
    // getters
    //
    Integer getLocalId();
    String getToRepoSlug();
    String getPullRequestName();
    String getPullRequestUrl();
    boolean isFoundIssueKey();

    //
    // setters
    //
    void setLocalId(Integer id);
    void setToRepoSlug(String repoSlug);
    void setPullRequestName(String name);
    void setPullRequestUrl(String url);
    void setFoundIssueKey(boolean foundIssueKey);
}

