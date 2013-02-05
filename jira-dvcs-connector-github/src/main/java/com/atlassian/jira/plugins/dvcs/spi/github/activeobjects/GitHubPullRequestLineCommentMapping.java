package com.atlassian.jira.plugins.dvcs.spi.github.activeobjects;

import java.util.Date;

import net.java.ao.Entity;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.Table;
import net.java.ao.schema.Unique;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestLineComment;

/**
 * AO mapping of the {@link GitHubPullRequestLineComment}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Table("GitHubPRLComment")
public interface GitHubPullRequestLineCommentMapping extends Entity
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
     * @see #getCommit()
     */
    String COLUMN_COMMIT = "COMMIT_ID";

    /**
     * @see #getPath()
     */
    String COLUMN_PATH = "PATH";

    /**
     * @see #getLine()
     */
    String COLUMN_LINE = "LINE";

    /**
     * @see #getText()
     */
    String COLUMN_TEXT = "TEXT";

    /**
     * @return {@link GitHubPullRequestLineComment#getGitHubId()}
     */
    @NotNull
    @Unique
    long getGitHubId();

    /**
     * @param gitHubId
     *            {@link #getGitHubId()}
     */
    void setGitHubId(long gitHubId);

    /**
     * @return {@link GitHubPullRequestLineComment#getGitHubId()}
     */
    @NotNull
    GitHubPullRequestMapping getPullRequest();

    /**
     * @param pullRequest
     *            {@link #getPullRequest()}
     */
    void setPullRequest(GitHubPullRequestMapping pullRequest);

    /**
     * @return {@link GitHubPullRequestLineComment#getCreatedAt()}
     */
    @NotNull
    Date getCreatedAt();

    /**
     * @param createdAt
     *            {@link #getCreatedAt()}
     */
    void setCreatedAt(Date createdAt);

    /**
     * @return {@link GitHubPullRequestLineComment#getCreatedBy()}
     */
    @NotNull
    GitHubUserMapping getCreatedBy();

    /**
     * @param createdBy
     *            {@link #getCreatedBy()}
     */
    void setCreatedBy(GitHubUserMapping createdBy);

    /**
     * @return {@link GitHubPullRequestLineComment#getCommit()}
     */
    @NotNull
    GitHubCommitMapping getCommit();

    /**
     * @param commit
     *            {@link #getCommit()}
     */
    void setCommit(GitHubCommitMapping commit);

    /**
     * @return {@link GitHubPullRequestLineComment#getPath()}
     */
    @NotNull
    String getPath();

    /**
     * @param path
     *            {@link #getPath()}
     */
    void setPath(String path);

    /**
     * @return {@link GitHubPullRequestLineComment#getLine()}
     */
    @NotNull
    int getLine();

    /**
     * @param line
     *            {@link #getLine()}
     */
    void setLine(int line);

    /**
     * @return {@link GitHubPullRequestLineComment#getText()}
     */
    @NotNull
    String getText();

    /**
     * @param text
     *            {@link #getText()}
     */
    void setText(String text);

}
