package com.atlassian.jira.plugins.dvcs.spi.github.activeobjects;

import java.util.Date;

import net.java.ao.Entity;
import net.java.ao.OneToOne;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.Table;
import net.java.ao.schema.Unique;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestComment;

/**
 * AO mapping of the {@link GitHubPullRequestComment}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Table("GitHubPRComment")
public interface GitHubPullRequestCommentMapping extends Entity
{

    /**
     * @see #getGitHubId()
     */
    String COLUMN_GIT_HUB_ID = "GIT_HUB_ID";

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
     * @see #getText()
     */
    String COLUMN_TEXT = "TEXT";

    /**
     * @return {@link GitHubPullRequestComment#getGitHubId()}.
     */
    @Unique
    @NotNull
    long getGitHubId();

    /**
     * @param gitHubId
     *            {@link #getGitHubId()}
     */
    void setGitHubId(long gitHubId);

    /**
     * @return {@link GitHubPullRequestComment#getPullRequest()}
     */
    GitHubPullRequestMapping getPullRequest();

    /**
     * @param pullRequest
     *            {@link #getPullRequest()}
     */
    @NotNull
    void setPullRequest(GitHubPullRequestMapping pullRequest);

    /**
     * @return {@link GitHubPullRequestComment#getCreatedAt()}
     */
    @NotNull
    Date getCreatedAt();

    /**
     * @param createdAt
     *            {@link #getCreatedAt()}
     */
    void setCreatedAt(Date createdAt);

    /**
     * @return {@link GitHubPullRequestComment#getCreatedBy()}
     */
    @NotNull
    GitHubUserMapping getCreatedBy();

    /**
     * @param createdBy
     *            {@link #getCreatedBy()}
     */
    @OneToOne
    void setCreatedBy(GitHubUserMapping createdBy);

    /**
     * @return {@link GitHubPullRequestComment#getText()}
     */
    String getText();

    /**
     * @param text
     *            {@link #getText()}
     */
    void setText(String text);

}
