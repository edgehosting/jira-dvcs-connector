package com.atlassian.jira.plugins.dvcs.spi.github.model;

import java.util.Date;

/**
 * GitHub user/organization.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubUser
{

    /**
     * @see #getId()
     */
    private int id;

    /**
     * 
     */
    private Date synchronizedAt;

    /**
     * @see #getGitHubId()
     */
    private int gitHubId;

    /**
     * @see #getLogin()
     */
    private String login;

    /**
     * @see #getName()
     */
    private String name;

    /**
     * @see #getEmail()
     */
    private String email;

    /**
     * @see #getUrl()
     */
    private String url;

    /**
     * @see #getAvatarUrl()
     */
    private String avatarUrl;

    /**
     * Constructor.
     */
    public GitHubUser()
    {
    }

    /**
     * @return Identity of this entity.
     */
    public int getId()
    {
        return id;
    }

    /**
     * @param id
     *            {@link #getId()}
     */
    public void setId(int id)
    {
        this.id = id;
    }

    /**
     * @return Date, when was last synchronized.
     */
    public Date getSynchronizedAt()
    {
        return synchronizedAt;
    }

    /**
     * @param synchronizedAt
     *            {@link #getSynchronizedAt()}
     */
    public void setSynchronizedAt(Date synchronizedAt)
    {
        this.synchronizedAt = synchronizedAt;
    }

    /**
     * @return GitHub identity of this entity.
     */
    public int getGitHubId()
    {
        return gitHubId;
    }

    /**
     * @param gitHubId
     *            {@link #getGitHubId()}
     */
    public void setGitHubId(int gitHubId)
    {
        this.gitHubId = gitHubId;
    }

    /**
     * @return GitHub login name of this user.
     */
    public String getLogin()
    {
        return login;
    }

    /**
     * @param login
     *            {@link #getLogin()}
     */
    public void setLogin(String login)
    {
        this.login = login;
    }

    /**
     * @return The name of the user/organzation.
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name
     *            {@link #getName()}
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return The user/organization e-mail.
     */
    public String getEmail()
    {
        return email;
    }

    /**
     * @param email
     *            {@link #getEmail()}
     */
    public void setEmail(String email)
    {
        this.email = email;
    }

    /**
     * @return URL to the user GitHub account.
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * @param url
     *            {@link #getUrl()}
     */
    public void setUrl(String url)
    {
        this.url = url;
    }

    /**
     * @return The appropriate avatar URL of this user/organization.
     */
    public String getAvatarUrl()
    {
        return avatarUrl;
    }

    /**
     * @param avatarUrl
     *            {@link #getAvatarUrl()}
     */
    public void setAvatarUrl(String avatarUrl)
    {
        this.avatarUrl = avatarUrl;
    }

}
