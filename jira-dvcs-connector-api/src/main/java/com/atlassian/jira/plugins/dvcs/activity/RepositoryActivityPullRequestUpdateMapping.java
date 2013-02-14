package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.OneToMany;
import net.java.ao.schema.Table;

@Table("PR_UPDATE")
public interface RepositoryActivityPullRequestUpdateMapping extends RepositoryActivityPullRequestMapping
{
    String STATUS = "STATUS";

    // Status constants
    enum Status
    {
    	APPROVED, OPENED, MERGED, DECLINED, UPDATED;
    }
    
    Status getStatus();

    @OneToMany
    RepositoryActivityCommitMapping[] getCommits();
    
    void setStatus(Status status);
}

