package com.atlassian.jira.plugins.dvcs.spi.github.activeobjects;

import java.util.Date;

import net.java.ao.schema.NotNull;
import net.java.ao.schema.Table;
import net.java.ao.schema.Unique;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommitComment;

/**
 * AO mapping of {@link GitHubCommitComment}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Table("GIT_HUB_C_COMMENT")
public interface GitHubCommitCommentMapping extends GitHubEntityMapping
{

    /**
     * @return {@link GitHubCommitComment#getGitHubId()}
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
     * @return {@link GitHubCommitComment#getCreatedAt()}
     */
    @NotNull
    Date getCreatedAt();

    /**
     * @param createdAt
     *            {@link #getCreatedAt()}
     */
    void setCreatedAt(Date createdAt);

    /**
     * @return {@link GitHubCommitComment#getCreatedBy()}
     */
    @NotNull
    GitHubUserMapping getCreatedBy();

    /**
     * @param createdBy
     *            {@link #getCreatedBy()}
     */
    void setCreatedBy(GitHubUserMapping createdBy);

    /**
     * @return {@link GitHubCommitComment#getCommit()}
     */
    GitHubCommitMapping getCommit();

    /**
     * @param commit
     *            {@link #getCommit()}
     */
    void setCommit(GitHubCommitMapping commit);

    /**
     * @return {@link GitHubCommitComment#getUrl()}
     */
    String getUrl();

    /**
     * @param url
     *            {@link #getUrl()}
     */
    void setUrl(String url);

    /**
     * 
     * @return {@link GitHubCommitComment#getHtmlUrl()}
     */
    String getHtmlUrl();

    /**
     * @param htmlUrl
     *            {@link #getHtmlUrl()}
     */
    void setHtmlUrl(String htmlUrl);

    /**
     * @return {@link GitHubCommitComment#getText()}
     */
    String getText();

    /**
     * @param text
     *            {@link #getText()}
     */
    void setText(String text);

}
