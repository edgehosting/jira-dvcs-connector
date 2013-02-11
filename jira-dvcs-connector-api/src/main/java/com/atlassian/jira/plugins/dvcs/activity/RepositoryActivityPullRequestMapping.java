package com.atlassian.jira.plugins.dvcs.activity;

import java.util.Date;

import net.java.ao.Entity;
import net.java.ao.Polymorphic;

@Polymorphic
public interface RepositoryActivityPullRequestMapping extends Entity 
{
    
    String ENTITY_TYPE = "ENTITY_TYPE";

    String LAST_UPDATED_ON = "LAST_UPDATED_ON";
    String REPO_ID = "REPO_ID";
    String INITIATOR_USERNAME = "author";
    String PULL_REQUEST_ID = "PULL_REQUEST_ID";
    
    //
    // getters
    //
    Date getLastUpdatedOn();
    int getRepoId();
    String getAuthor();
    String getPullRequestId();

    //
    // setters
    //
    void setLastUpdatedOn(Date date);
    void setRepoId(int repodId);
    void setAuthor(String username);
    void setPullRequestId(Integer pullRequestId);
    
}

