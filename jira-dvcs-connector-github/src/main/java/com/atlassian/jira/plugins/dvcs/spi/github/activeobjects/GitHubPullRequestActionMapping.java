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
