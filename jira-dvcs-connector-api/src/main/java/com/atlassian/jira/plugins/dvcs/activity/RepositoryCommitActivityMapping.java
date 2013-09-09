package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.Polymorphic;
import net.java.ao.schema.NotNull;

/**
 * Base of {@link RepositoryPullRequestUpdateActivityToCommitMapping commit} related activities.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Polymorphic
public interface RepositoryCommitActivityMapping extends RepositoryActivityMapping
{

    /**
     * @see #getCommit()
     */
    String COMMIT = "COMMIT_ID";

    /**
     * @return over, which commit is this activity
     */
    @NotNull
    RepositoryCommitMapping getCommit();

    /**
     * @param commit
     *            {@link #getCommit()}
     */
    void setCommit(RepositoryCommitActivityMapping commit);

}
