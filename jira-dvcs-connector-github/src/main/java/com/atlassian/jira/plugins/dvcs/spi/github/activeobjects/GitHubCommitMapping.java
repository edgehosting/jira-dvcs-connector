package com.atlassian.jira.plugins.dvcs.spi.github.activeobjects;

import java.util.Date;

import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.util.NotNull;

/**
 * AO of the {@link GitHubCommit}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Table("GitHubCommit")
public interface GitHubCommitMapping extends GitHubEntityMapping
{

    /**
     * @return {@link GitHubCommit#getRepository()}
     */
    @NotNull
    GitHubRepositoryMapping getRepository();

    /**
     * @param repository
     *            {@link #getRepository()}
     */
    void setRepository(GitHubRepositoryMapping repository);

    /**
     * @return {@link GitHubCommit#getSha()}
     */
    @NotNull
    String getSha();

    /**
     * @param sha
     *            {@link #getSha()}
     */
    void setSha(String sha);

    /**
     * @return {@link GitHubCommit#getCreatedAt()}
     */
    @NotNull
    Date getCreatedAt();

    /**
     * @param createdAt
     *            {@link #getCreatedAt()}
     */
    void setCreatedAt(Date createdAt);

    /**
     * @return {@link GitHubCommit#getCreatedBy()}
     */
    @NotNull
    String getCreatedBy();

    /**
     * @param createdBy
     *            {@link #getCreatedBy()}
     */
    void setCreatedBy(String createdBy);

    /**
     * @return {@link GitHubCommit#getCreatedByName()}
     */
    String getCreatedByName();

    /**
     * @param createdByName
     *            {@link #getCreatedByName()}
     */
    void setCreatedByName(String createdByName);

    /**
     * @return {@link GitHubCommit#getCreatedByAvatarUrl()}
     */
    String getCreatedByAvatarUrl();

    /**
     * @param createdByAvatarUrl
     *            {@link #getCreatedByAvatarUrl()}
     */
    void setCreatedByAvatarUrl(String createdByAvatarUrl);

    /**
     * @return {@link GitHubCommit#getHtmlUrl()}
     */
    String getHtmlUrl();

    /**
     * @param htmlUrl
     *            {@link #getHtmlUrl()}
     */
    void setHtmlUrl(String htmlUrl);

    /**
     * @return {@link GitHubCommit#getMessage()}
     */
    @StringLength(StringLength.UNLIMITED)
    String getMessage();

    /**
     * @param message
     *            {@link #getMessage()}
     */
    void setMessage(String message);

}
