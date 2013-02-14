package com.atlassian.jira.plugins.dvcs.spi.github.activeobjects;

import net.java.ao.schema.Table;

/**
 * Relation holder of the {@link GitHubPushMapping#getCommits()}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Table("GitHubP_Commit")
public interface GitHubPushCommitMapping extends GitHubEntityMapping
{

    /**
     * @return assigned {@link #getCommit()}
     */
    GitHubPushMapping getPush();

    /**
     * @param push
     *            {@link #getPush()}
     */
    void setPush(GitHubPushMapping push);

    /**
     * @return {@link GitHubPushMapping#getCommits()}
     */
    GitHubCommitMapping getCommit();

    /**
     * @param commit
     *            {@link #getCommit()}
     */
    void setCommit(GitHubCommitMapping commit);

}
