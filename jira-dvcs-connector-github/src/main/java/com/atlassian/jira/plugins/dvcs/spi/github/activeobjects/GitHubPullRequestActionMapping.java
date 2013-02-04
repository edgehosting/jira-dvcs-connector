package com.atlassian.jira.plugins.dvcs.spi.github.activeobjects;

import java.util.Date;

import net.java.ao.Entity;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.Table;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestAction;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestAction.Action;

/**
 * AO mapping of the {@link GitHubPullRequestAction}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Table("GitHubPRAction")
public interface GitHubPullRequestActionMapping extends Entity
{

    /**
     * @see #getGitHubEventId()
     */
    String COLUMN_GIT_HUB_EVENT_ID = "GIT_HUB_EVENT_ID";

    /**
     * @see #getPullRequest()
     */
    String COLUMN_PULL_REQUEST = "PULL_REQUEST_ID";
    
    /**
     * @see #getCreatedAt()
     */
    String COLUMN_CREATED_AT = "CREATED_AT";

    /**
     * @see #getCreatedBy()
     */
    String COLUMN_CREATED_BY = "CREATED_BY_ID";

    /**
     * @see #getAction()
     */
    String COLUMN_ACTION = "ACTION";

    /**
     * @return {@link GitHubPullRequestAction#getGitHubEventId()}
     */
    @NotNull
    String getGitHubEventId();

    /**
     * @param gitHubEventId
     *            {@link #getGitHubEventId()}
     */
    void setGitHubEventId(String gitHubEventId);

    /**
     * @return {@link GitHubPullRequestMapping#getActions()}
     */
    GitHubPullRequestMapping getPullRequest();

    /**
     * @param pullRequest
     *            {@link #getPullRequest()}
     */
    void setPullRequest(GitHubPullRequestMapping pullRequest);

    /**
     * @return {@link GitHubPullRequestAction#getCreatedAt()}
     */
    @NotNull
    Date getCreatedAt();

    /**
     * @param createdAt
     *            {@link #getCreatedAt()}
     */
    void setCreatedAt(Date createdAt);

    /**
     * @return {@link GitHubPullRequestAction#getCreatedBy()}
     */
    GitHubUserMapping getCreatedBy();

    /**
     * @param createdBy
     *            {@link #getCreatedBy()}
     */
    void setCreatedBy(GitHubUserMapping createdBy);

    /**
     * @return {@link GitHubPullRequestAction#getAction()}
     */
    @NotNull
    Action getAction();

    /**
     * @param action
     *            {@link #getAction()}
     */
    void setAction(Action action);

}
