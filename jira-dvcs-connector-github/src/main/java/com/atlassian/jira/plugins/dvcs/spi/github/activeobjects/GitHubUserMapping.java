package com.atlassian.jira.plugins.dvcs.spi.github.activeobjects;

import java.util.Date;

import net.java.ao.schema.Table;
import net.java.ao.schema.Unique;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubUser;
import com.atlassian.jira.util.NotNull;

/**
 * AO representation of the {@link GitHubUser}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Table("GitHubUser")
public interface GitHubUserMapping extends GitHubEntityMapping
{

    /**
     * @return {@link GitHubUser#getSynchronizedAt()}
     */
    @NotNull
    Date getSynchronizedAt();

    /**
     * @param synchronizedAt
     *            {@link #getSynchronizedAt()}
     */
    void setSynchronizedAt(Date synchronizedAt);

    /**
     * @return {@link GitHubUser#getGitHubId()}
     */
    @Unique
    @NotNull
    int getGitHubId();

    /**
     * @param gitHubId
     *            {@link #getGitHubId()}
     */
    void setGitHubId(int gitHubId);

    /**
     * @return {@link GitHubUser#getLogin()}
     */
    @Unique
    @NotNull
    String getLogin();

    /**
     * @param login
     *            {@link #getLogin()}
     */
    void setLogin(String login);

    /**
     * @return {@link GitHubUser#getName()}
     */
    String getName();

    /**
     * @param name
     *            {@link #getName()}
     */
    void setName(String name);

    /**
     * @return {@link GitHubUser#getEmail()}
     */
    String getEmail();

    /**
     * @param email
     *            {@link #getEmail()}
     */
    void setEmail(String email);

    /**
     * @return {@link GitHubUser#getUrl()}
     */
    String getUrl();

    /**
     * @param url
     *            {@link #getUrl()}
     */
    void setUrl(String url);

    /**
     * @return {@link GitHubUser#getAvatarUrl()}
     */
    String getAvatarUrl();

    /**
     * @param avatarUrl
     *            {@link #getAvatarUrl(String)}
     */
    void setAvatarUrl(String avatarUrl);

}
