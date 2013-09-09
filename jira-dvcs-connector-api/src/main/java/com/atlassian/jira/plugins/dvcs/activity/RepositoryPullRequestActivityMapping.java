package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.Polymorphic;
import net.java.ao.schema.NotNull;

@Polymorphic
public interface RepositoryPullRequestActivityMapping extends RepositoryActivityMapping
{

    String PULL_REQUEST_ID = "PULL_REQUEST_ID";

    //
    // getters
    //
    @NotNull
    RepositoryPullRequestMapping getPullRequest();

    //
    // setters
    //
    void setPullRequest(RepositoryPullRequestMapping pullRequest);
}
