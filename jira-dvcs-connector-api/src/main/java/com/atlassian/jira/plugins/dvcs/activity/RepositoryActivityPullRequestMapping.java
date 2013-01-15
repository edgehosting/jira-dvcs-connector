package com.atlassian.jira.plugins.dvcs.activity;

import java.util.Date;

import net.java.ao.Entity;

public interface RepositoryActivityPullRequestMapping extends Entity {
    
    String ENTITY_TYPE = "ENTITY_TYPE";

    String ISSUE_KEY = "ISSUE_KEY";
    String LAST_UPDATED_ON = "LAST_UPDATED_ON";
    String REPO_SLUG = "REPO_SLUG";
    String INITIATOR_USERNAME = "INITIATOR_USERNAME";
    String PULL_REQUEST_NAME = "PULL_REQUEST_NAME";
    String PULL_REQUEST_URL = "PULL_REQUEST_URL";
    
    //
    // getters
    //
    String getIssueKey();
    Date getLastUpdatedOn();
    String getRepoSlug();
    String getInitiatorUsername();
    String getPullRequestName();
    String getPullRequestUrl();

    //
    // setters
    //
    void setIssueKey(String issueKey);
    void setLastUpdatedOn(Date date);
    void setRepoSlug(String slug);
    void setInitiatorUsername(String username);
    void setPullRequestName(String name);
    void setPullRequestUrl(String url);
    
}

