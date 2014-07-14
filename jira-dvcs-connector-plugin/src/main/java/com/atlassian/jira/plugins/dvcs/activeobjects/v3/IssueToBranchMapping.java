package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.Table;

@Preload
@Table("IssueToBranch")
public interface IssueToBranchMapping extends Entity
{

    public static final String BRANCH_ID = "BRANCH_ID";
    public static final String ISSUE_KEY = "ISSUE_KEY";


    BranchMapping getBranch();
    @Indexed
    String getIssueKey();

    void setBranch(BranchMapping branch);
    void setIssueKey(String issueKey);

}
