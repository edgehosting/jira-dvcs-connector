package com.atlassian.jira.plugins.dvcs.spi.github.activeobjects;

import java.util.Date;

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
public interface GitHubPullRequestCommentMapping extends GitHubEntityMapping
{

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
