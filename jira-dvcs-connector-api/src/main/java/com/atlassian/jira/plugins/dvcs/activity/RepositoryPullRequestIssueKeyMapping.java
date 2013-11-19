package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.Preload;
import net.java.ao.schema.Table;

@Preload
@Table("PR_ISSUE_KEY")
public interface RepositoryPullRequestIssueKeyMapping extends RepositoryDomainMapping
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
