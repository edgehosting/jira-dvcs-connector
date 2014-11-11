package com.atlassian.jira.plugins.dvcs.spi.github.service;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import org.eclipse.egit.github.core.PullRequest;

import java.util.Set;

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
