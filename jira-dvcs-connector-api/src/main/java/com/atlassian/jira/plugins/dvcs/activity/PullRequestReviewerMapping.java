package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.schema.Table;

@Table("PR_REVIEWER")
public interface PullRequestReviewerMapping extends RepositoryDomainMapping
{
    String USERNAME = "USERNAME";
    String APPROVED = "APPROVED";
    String PULL_REQUEST_ID = "PULL_REQUEST_ID";

    //
    // getters
    //
    /**
     * @return username of the reviewer
     */
    String getUsername();

    /**
     * @return whether the reviewer approved the pull request
     */
    boolean isApproved();

    RepositoryPullRequestMapping getPullRequest();
    //
    // setters
    //
    void setUsername(String username);

    void setApproved(boolean approved);
}
