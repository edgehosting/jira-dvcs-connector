package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

@Table("PR_ISSUE_KEY")
public interface RepositoryPullRequestIssueKeyMapping extends Entity
{
    
    String PULL_REQUEST_ID = "PULL_REQUEST_ID";
    String ISSUE_KEY = "ISSUE_KEY";
    
    //
    // getters
    //
    int getPullRequestId();
    String getIssueKey();

    //
    // setters
    //
    void setPullRequestId(int pullRequestId);
    void setIssueKey(String issueKey);
}

