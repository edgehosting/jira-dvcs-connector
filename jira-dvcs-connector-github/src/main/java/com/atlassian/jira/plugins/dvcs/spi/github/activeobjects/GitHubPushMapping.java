package com.atlassian.jira.plugins.dvcs.spi.github.activeobjects;

import java.util.Date;

import net.java.ao.OneToMany;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.Table;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPush;

/**
 * AO of the {@link GitHubPush}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Table("GitHubPush")
public interface GitHubPushMapping extends GitHubEntityMapping
{

    /**
     * @return {@link GitHubPush#getCreatedAt()}
     */
    @NotNull
    Date getCreatedAt();

    /**
     * @param createdAt
     *            {@link #getCreatedAt()}
     */
    void setCreatedAt(Date createdAt);

    /**
     * @return {@link GitHubPush#getCreatedBy()}
     */
    @NotNull
    GitHubUserMapping getCreatedBy();

    /**
     * @param gitHubUser
     *            {@link #getCreatedBy()}
     */
    void setCreatedBy(GitHubUserMapping gitHubUser);

    /**
     * @return {@link GitHubPush#getRepository()}
     */
    @NotNull
    GitHubRepositoryMapping getRepository();

    /**
     * @param repository
     *            {@link #getRepository()}
     */
    void setRepository(GitHubRepositoryMapping repository);

    /**
     * 
     * @return {@link GitHubPush#getRef()}
     */
    @NotNull
    String getRef();

    /**
     * @param ref
     *            {@link #getRef()}
     */
    void setRef(String ref);

    /**
     * 
     * @return {@link GitHubPush#getBefore()}
     */
    @NotNull
    String getBefore();

    /**
     * @param before
     *            {@link #getBefore()}
     */
    void setBefore(String before);

    /**
     * 
     * @return {@link GitHubPush#getHead()}
     */
    @NotNull
    String getHead();

    /**
     * @param head
     *            {@link #getHead()}
     */
    void setHead(String head);

    /**
     * @return {@link GitHubPush#getCommits()}
     */
    @OneToMany
    GitHubPushCommitMapping[] getCommits();

}
