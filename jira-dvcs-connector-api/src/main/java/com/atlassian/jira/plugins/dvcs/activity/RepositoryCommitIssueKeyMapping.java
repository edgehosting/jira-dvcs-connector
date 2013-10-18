package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.schema.Table;

/**
 * Holds relation between issue key and appropriate {@link RepositoryPullRequestUpdateActivityToCommitMapping}, respectively
 * {@link RepositoryPullRequestUpdateActivityToCommitMapping} and appropriate issue key.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Table("COMMIT_ISSUE_KEY")
public interface RepositoryCommitIssueKeyMapping extends RepositoryDomainMapping
{

    /**
     * @see #getCommit()
     */
    String COMMIT = "COMMIT_ID";

    /**
     * @see #getIssueKey()
     */
    String ISSUE_KEY = "ISSUE_KEY";

    /**
     * @return Related commit.
     */
    RepositoryCommitMapping getCommit();

    /**
     * @param commit
     *            {@link #getCommit()}
     */
    void setCommit(RepositoryCommitMapping commit);

    /**
     * @return Issue key related to the provided {@link #getCommit()}
     */
    String getIssueKey();

    /**
     * @param issueKey
     *            {@link #getIssueKey()}
     */
    void setIssueKey(String issueKey);
}
