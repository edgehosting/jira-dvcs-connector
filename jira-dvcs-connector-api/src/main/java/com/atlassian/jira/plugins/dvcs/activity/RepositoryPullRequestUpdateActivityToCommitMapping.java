package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.schema.NotNull;
import net.java.ao.schema.Table;

/**
 * Represents single repository commit.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Table("ACTIVITY_PR_U_TO_C")
public interface RepositoryPullRequestUpdateActivityToCommitMapping extends RepositoryDomainMapping
{

    /**
     * @see #getActivity()
     */
    String ACTIVITY = "ACTIVITY_ID";

    /**
     * @see #getCommit()
     */
    String COMMIT = "COMMIT_ID";

    /**
     * @return activity of commit
     */
    @NotNull
    RepositoryPullRequestUpdateActivityMapping getActivity();

    /**
     * @param activity
     *            {@link #getActivity()}
     */
    void setActivity(RepositoryPullRequestUpdateActivityMapping activity);

    /**
     * @return commit of {@link #getActivity()}
     */
    @NotNull
    RepositoryCommitMapping getCommit();

    /**
     * @param commit
     *            {@link #getCommit()}
     */
    void setCommit(RepositoryCommitMapping commit);

}
