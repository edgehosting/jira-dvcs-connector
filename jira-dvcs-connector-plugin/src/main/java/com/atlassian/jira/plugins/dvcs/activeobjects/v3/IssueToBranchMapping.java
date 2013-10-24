package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

@Table("IssueToBranch")
public interface IssueToBranchMapping extends Entity
{

    public static final String BRANCH_ID = "BRANCH_ID";
    public static final String ISSUE_KEY = "ISSUE_KEY";


    BranchMapping getBranch();
    String getIssueKey();

    void getBranch(BranchMapping changeset);
    void setIssueKey(String issueKey);

}
