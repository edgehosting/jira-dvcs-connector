package com.atlassian.jira.plugins.dvcs.spi.github.service;

import org.eclipse.egit.github.core.PullRequest;

/**
 * Interface defining context for GitHub event synchronisation
 */
public interface GitHubEventContext
{
    /**
     * Saving pull request
     */

    void savePullRequest(PullRequest pullRequest);
}
