package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.schema.NotNull;
import net.java.ao.schema.Table;

/**
 * Represents single repository commit.
 *
 * @author Stanislav Dvorscak
 *
 */
@Table("PR_TO_COMMIT")
public interface RepositoryPullRequestToCommitMapping extends RepositoryDomainMapping
{

    String REQUEST_ID = "REQUEST_ID";

    String COMMIT = "COMMIT_ID";

    @NotNull
    RepositoryPullRequestMapping getRequest();

    void setRequest(RepositoryPullRequestMapping activity);

    @NotNull
    RepositoryCommitMapping getCommit();

    void setCommit(RepositoryCommitMapping commit);

}
