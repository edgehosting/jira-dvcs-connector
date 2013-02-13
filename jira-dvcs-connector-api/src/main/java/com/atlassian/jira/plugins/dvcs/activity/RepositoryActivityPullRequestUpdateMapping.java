package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.OneToMany;
import net.java.ao.schema.Table;

@Table("PR_UPDATE")
public interface RepositoryActivityPullRequestUpdateMapping extends RepositoryActivityPullRequestMapping
{
    String STATUS = "STATUS";
    
    String getStatus();
    @OneToMany
    RepositoryActivityCommitMapping[] getCommits();
    
    void setStatus(String status);
}

