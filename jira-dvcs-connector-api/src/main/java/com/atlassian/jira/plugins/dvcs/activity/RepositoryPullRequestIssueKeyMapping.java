package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

@Table("PrIssueKey")
public interface RepositoryPullRequestIssueKeyMapping extends Entity {
    
    String PULL_REQUEST_ID = "PULL_REQUEST_ID";
    String ISSUE_KEY = "ISSUE_KEY";
    
    //
    // getters
    //
    Integer getPullRequestId();
    String getIssueKey();

    //
    // setters
    //
    void setPullRequestId(Integer prId);
    void setIssueKey(String issueKey);
}

