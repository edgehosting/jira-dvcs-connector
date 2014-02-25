package com.atlassian.jira.plugins.dvcs.spi.github.service;

/**
 * Interface defining context for GitHub event synchronisation
 */
public interface GitHubEventContext
{
    /**
     * Saving pull request
     * 
     * @param pullRequestId
     *            GitHub id of pull request
     * @param pullRequestNumber
     *            GitHub number of pull request
     */
    void savePullRequest(long pullRequestId, int pullRequestNumber);
}
