package com.atlassian.jira.plugins.dvcs.spi.github.activeobjects;

import java.util.Date;

import net.java.ao.schema.NotNull;
import net.java.ao.schema.Table;
import net.java.ao.schema.Unique;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommitLineComment;

/**
 * AO mapping of {@link GitHubCommitLineComment}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Table("GIT_HUB_C_L_COMMENT")
public interface GitHubCommitLineCommentMapping extends GitHubEntityMapping
{

    /**
     * @return {@link GitHubCommitLineComment#getGitHubId()}
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
     * @return {@link GitHubCommitLineComment#getCommit()}
     */
    @NotNull
    GitHubCommitMapping getCommit();

    /**
     * @param commit
     *            {@link #getCommit()}
     */
    void setCommit(GitHubCommitMapping commit);

    /**
     * @return {@link GitHubCommitLineComment#getUrl()}
     */
    String getUrl();

    /**
     * @param url
     *            {@link #getUrl()}
     */
    void setUrl(String url);

    /**
     * 
     * @return {@link GitHubCommitLineComment#getHtmlUrl()}
     */
    String getHtmlUrl();

    /**
     * @param htmlUrl
     *            {@link #getHtmlUrl()}
     */
    void setHtmlUrl(String htmlUrl);

    /**
     * @return {@link GitHubCommitLineComment#getCreatedAt()}
     */
    Date getCreatedAt();

    /**
     * @param createdAt
     *            {@link #getCreatedAt()}
     */
    void setCreatedAt(Date createdAt);

    /**
     * @return {@link GitHubCommitLineComment#getCreatedBy()}
     */
    @NotNull
    GitHubUserMapping getCreatedBy();

    /**
     * @param createdBy
     *            {@link #getCreatedBy()}
     */
    void setCreatedBy(GitHubUserMapping createdBy);

    /**
     * @return {@link GitHubCommitLineComment#getPath()}
     */
    String getPath();

    /**
     * @param path
     *            {@link #getPath()}
     */
    void setPath(String path);

    /**
     * @return {@link GitHubCommitLineComment#getLine()}
     */
    int getLine();

    /**
     * @param line
     *            {@link #getLine()}
     */
    void setLine(int line);

    /**
     * @return {@link GitHubCommitLineComment#getText()}
     */
    String getText();

    /**
     * @param text
     *            {@link #getText()}
     */
    void setText(String text);

}
