package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 *
 * BitbucketChangeset
 *
 * <pre>
 * {
 *   "hash": "2c10238df694c8809b56dc7a9b7fe19d4d555c1a",
 *   "repository": {
 *      "links": [...],
 *      "full_name": "bitbucket/bitbucket"
 *   },
 *   "links": [...],
 *   "author": {
 *     "raw": "John Doe &lt;john.doe@gmail.com&gt;",
 *     "user": {
 *       "username": "jdoe",
 *       "display_name": "John Doe",
 *       "links": [...]
 *     }
 *   },
 *   "parents": [
 *     {
 *       "hash": "4c3b7e9e13d425cfba1cee4e82f72f8b2c812512",
 *       "links": [...]
 *     }
 *   ],
 *   "date": "2013-06-17T03:49:13+00:00",
 *   "message": "convert BB.delay to AMD"
 * },
 * </pre>
 */
public class BitbucketNewChangeset implements Serializable
{
    private static final long serialVersionUID = 4500500360628759017L;

    private String hash;
    private BitbucketRepositoryInfo repository;
    private BitbucketAuthor author;
    private List<BitbucketNewChangeset> parents;
    private Date date;
	private String message;
	private String branch;

    public String getHash()
    {
        return hash;
    }

    public void setHash(String hash)
    {
        this.hash = hash;
    }

    public BitbucketRepositoryInfo getRepository()
    {
        return repository;
    }

    public void setRepository(BitbucketRepositoryInfo repository)
    {
        this.repository = repository;
    }

    public BitbucketAuthor getAuthor()
    {
        return author;
    }

    public void setAuthor(BitbucketAuthor author)
    {
        this.author = author;
    }

    public List<BitbucketNewChangeset> getParents()
    {
        return parents;
    }

    public void setParents(List<BitbucketNewChangeset> parents)
    {
        this.parents = parents;
    }

    public Date getDate()
    {
        return date;
    }

    public void setDate(Date date)
    {
        this.date = date;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public String getBranch()
    {
        return branch;
    }

    public void setBranch(String branch)
    {
        this.branch = branch;
    }
}
