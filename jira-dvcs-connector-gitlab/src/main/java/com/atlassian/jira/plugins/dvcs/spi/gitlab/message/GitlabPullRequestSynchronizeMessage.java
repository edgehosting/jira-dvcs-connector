package com.atlassian.jira.plugins.dvcs.spi.gitlab.message;

import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.BaseProgressEnabledMessage;

/**
 * Message which is fired when a GitHub {@link PullRequest} should be updated.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitlabPullRequestSynchronizeMessage extends BaseProgressEnabledMessage
{

    /**
     * @see #getPullRequestNumber()
     */
    private final int pullRequestNumber;

    /**
     * Constructor.
     *
     * @param progress
     *            {@link #getProgress()}
     * @param syncAuditId
     *            {@link #getSyncAuditId()}
     * @param softSync
     *            {@link #isSoftSync()}
     * @param repository
     *            {@link #getRepository()}
     * @param pullRequestNumber
     *            {@link #getPullRequestNumber()}
     * @param webHookSync
     *            whether this sync was triggered by a web hook
     */
    public GitlabPullRequestSynchronizeMessage(Progress progress, int syncAuditId, boolean softSync, Repository repository,
            int pullRequestNumber, boolean webHookSync)
    {
        super(progress, syncAuditId, softSync, repository, webHookSync);
        this.pullRequestNumber = pullRequestNumber;
    }

    /**
     * @return Remote ID of pull request
     */
    public int getPullRequestNumber()
    {
        return pullRequestNumber;
    }
}
