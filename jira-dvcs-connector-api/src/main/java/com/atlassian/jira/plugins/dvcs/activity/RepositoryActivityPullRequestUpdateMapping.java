package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.schema.Table;

@Table("PrUpdate")
public interface RepositoryActivityPullRequestUpdateMapping extends RepositoryActivityPullRequestMapping
{
    String STATUS = "STATUS";
    
    String getStatus();
    
    void setStatus(String status);
}

