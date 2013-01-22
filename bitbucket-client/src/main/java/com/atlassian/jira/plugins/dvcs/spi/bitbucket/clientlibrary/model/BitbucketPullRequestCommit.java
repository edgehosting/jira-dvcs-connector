package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;
import java.util.Date;

/**
 *
 * 
 * <br /><br />
 * Created on 11.12.2012, 14:02:57
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class BitbucketPullRequestCommit implements Serializable
{

    private static final long serialVersionUID = 8212352604704981087L;

    private BitbucketPullRequestCommitAuthor author;
    private String href;
    private Date date;
    private String message;

    public BitbucketPullRequestCommit()
    {
        super();
    }

    public String getHref()
    {
        return href;
    }

    public void setHref(String href)
    {
        this.href = href;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public BitbucketPullRequestCommitAuthor getAuthor()
    {
        return author;
    }

    public void setAuthor(BitbucketPullRequestCommitAuthor author)
    {
        this.author = author;
    }

    public Date getDate()
    {
        return date;
    }

    public void setDate(Date date)
    {
        this.date = date;
    }

}

