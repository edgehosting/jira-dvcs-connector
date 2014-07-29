package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

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
    private BitbucketLinks links;
    private Date date;
    private String message;
    private String hash;
    // Used in BitbucketPullRequestBaseActivity.source.commit
    private String sha;
    private List<BitbucketNewChangeset> parents;

    public BitbucketPullRequestCommit()
    {
        super();
    }

    public BitbucketLinks getLinks()
    {
        return links;
    }

    public void setLinks(final BitbucketLinks links)
    {
        this.links = links;
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

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

    public String getSha()
    {
        return sha;
    }

    public void setSha(final String sha)
    {
        this.sha = sha;
    }

    public List<BitbucketNewChangeset> getParents()
    {
        return parents;
    }

    public void setParents(final List<BitbucketNewChangeset> parents)
    {
        this.parents = parents;
    }
}

