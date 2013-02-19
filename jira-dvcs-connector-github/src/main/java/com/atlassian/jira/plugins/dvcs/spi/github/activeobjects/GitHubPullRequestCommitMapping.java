package com.atlassian.jira.plugins.dvcs.spi.github.activeobjects;

import net.java.ao.schema.Table;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;

/**
 * Defines association between {@link GitHubPullRequest} and {@link GitHubCommit} - {@link GitHubPullRequest#getCommits()}
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Table("GIT_HUB_PR_COMMIT")
public interface GitHubPullRequestCommitMapping extends GitHubEntityMapping
{
    
    /**
     * @return Associated {@link GitHubPullRequest}.
     */
    GitHubPullRequestMapping getPullRequest();

    /**
     * @param pullRequest
     *            {@link #getPullRequest()}
     */
    void setPullRequest(GitHubPullRequestMapping pullRequest);

    /**
     * @return Associated {@link GitHubCommit}.
     */
    GitHubCommitMapping getCommit();

    /**
     * @param commit
     *            {@link #getCommit()}
     */
    void setCommit(GitHubCommitMapping commit);

}
