package com.atlassian.jira.plugins.dvcs.activity;

import java.util.Date;

import net.java.ao.Entity;
import net.java.ao.Polymorphic;
import net.java.ao.schema.NotNull;

@Polymorphic
public interface RepositoryActivityPullRequestMapping extends Entity
{

    String ENTITY_TYPE = "ENTITY_TYPE";

    String LAST_UPDATED_ON = "LAST_UPDATED_ON";
    String REPOSITORY_ID = "REPOSITORY_ID";
    String AUTHOR = "AUTHOR";
    String RAW_AUTHOR = "RAW_AUTHOR";
    String PULL_REQUEST_ID = "PULL_REQUEST_ID";

    //
    // getters
    //
    @NotNull
    Date getLastUpdatedOn();

    @NotNull
    int getRepositoryId();

    @NotNull
    String getAuthor();
    
    @NotNull
    String getRawAuthor();

    @NotNull
    RepositoryPullRequestMapping getPullRequest();

    //
    // setters
    //
    void setLastUpdatedOn(Date date);

    void setRepositoryId(int repositoryId);

    void setAuthor(String username);

    void setRawAuthor(String rawAuthor);

    void setPullRequest(RepositoryPullRequestMapping pullRequest);

}
