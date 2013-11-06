package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.schema.Table;

@Table("PR_PARTICIPANT")
public interface PullRequestParticipantMapping extends RepositoryDomainMapping
{
    String USERNAME = "USERNAME";
    String APPROVED = "APPROVED";
    String ROLE = "ROLE";
    String PULL_REQUEST_ID = "PULL_REQUEST_ID";

    //
    // getters
    //
    /**
     * @return username of the participant
     */
    String getUsername();

    /**
     * @return whether the participant approved the pull request
     */
    boolean isApproved();

    /**
     * @return role of the participant
     */
    String getRole();

    RepositoryPullRequestMapping getPullRequest();
    //
    // setters
    //
    void setUsername(String username);

    void setApproved(boolean approved);

    void setRole(String role);
}
