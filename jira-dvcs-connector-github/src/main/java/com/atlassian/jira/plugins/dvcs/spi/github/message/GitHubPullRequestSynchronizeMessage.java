package com.atlassian.jira.plugins.dvcs.spi.github.message;

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
public class GitHubPullRequestSynchronizeMessage extends BaseProgressEnabledMessage
{

    /**
     * @see #getPullRequestNumber()
     */
    private final int pullRequestNumber;

    /**
     * @see #getChangeType()
     */
    private final ChangeType changeType;

    /**
     * @see #getChangeType()
     * @author Stanislav Dvorscak
     * 
     */
    public static enum ChangeType
    {
        
        PULL_REQUEST,
        PULL_REQUEST_COMMENT, 
        PULL_REQUEST_REVIEW_COMMENT, 

    }

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
     */
    public GitHubPullRequestSynchronizeMessage(Progress progress, int syncAuditId, boolean softSync, Repository repository,
            int pullRequestNumber, ChangeType changeType)
    {
        super(progress, syncAuditId, softSync, repository);
        this.pullRequestNumber = pullRequestNumber;
        this.changeType = changeType;
    }

    /**
     * @return Remote ID of pull request
     */
    public int getPullRequestNumber()
    {
        return pullRequestNumber;
    }

    /**
     * @return type of change, which was realized on pull request.
     */
    public ChangeType getChangeType()
    {
        return changeType;
    }

}
