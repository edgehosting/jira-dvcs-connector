package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.Preload;
import net.java.ao.schema.Table;

@Preload
@Table("PrComment")
public interface RepositoryActivityPullRequestCommentMapping extends RepositoryActivityPullRequestMapping {
    
  
    
}

