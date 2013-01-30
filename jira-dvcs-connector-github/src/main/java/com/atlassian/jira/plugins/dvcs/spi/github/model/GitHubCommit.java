package com.atlassian.jira.plugins.dvcs.spi.github.model;

import java.util.Date;

/**
 * Model of the commit.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubCommit
{

    /**
     * @see #getId()
     */
    private int id;

    /**
     * @see #getSha()
     */
    private String sha;

    /**
     * @see #getDate()
     */
    private Date date;

    /**
     * @see #getAuthor()
     */
    private String author;

    /**
     * @see #getMessage()
     */
    private String message;

    /**
     * Constructor.
     */
    public GitHubCommit()
    {
    }

    /**
     * @return Identity of the commit.
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
     * @return SHA id of the GitHub/Git commit.
     */
    public String getSha()
    {
        return sha;
    }

    /**
     * @param sha
     *            {@link #getSha()}
     */
    public void setSha(String sha)
    {
        this.sha = sha;
    }

    /**
     * @return The date when the commit was introduced.
     */
    public Date getDate()
    {
        return date;
    }

    /**
     * @param date
     *            {@link #getDate()}
     */
    public void setDate(Date date)
    {
        this.date = date;
    }

    /**
     * @return Author of the commit.
     */
    public String getAuthor()
    {
        return author;
    }

    /**
     * @param author
     *            {@link #getAuthor()}
     */
    public void setAuthor(String author)
    {
        this.author = author;
    }

    /**
     * @return A message related to the commit.
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * @param message
     *            {@link #getMessage()}
     */
    public void setMessage(String message)
    {
        this.message = message;
    }

}
