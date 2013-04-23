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
 *     "links": [
 *         {
 *             "href": "/api/1.0/repositories/mstencel/test-git/changesets/b50c32732daa539cf4a00831286f0490fd8981a0",
 *             "rel": "self"
 *         }
 *     ],
 *     "author": {
 *         "raw": "Stanislav Dvorscak <stanislav-dvorscak@solumiss.eu>"
 *     },
 *     "sha": "b50c32732daa539cf4a00831286f0490fd8981a0",
 *     "parents": [
 *         {
 *             "sha": "5447d064602e89aa1ce381c6c17ca8cab385fdf1",
 *             "links": [
 *                 {
 *                     "href": "/api/1.0/repositories/mstencel/test-git/changesets/5447d064602e89aa1ce381c6c17ca8cab385fdf1",
 *                     "rel": "self"
 *                 }
 *             ]
 *         }
 *     ],
 *     "date": "2013-04-12T14:55:59+00:00",
 *     "message": "Close branch issue/BBC-472\n"
 * }
 * </pre>
 *
 * 
 * <br /><br />
 *
 * @author Miroslav Stencel <mstencel@atlassian.com>
 *
 */
public class BitbucketNewChangeset implements Serializable
{
    private static final long serialVersionUID = 4500500360628759017L;

    private String sha;
	private String message;
    private String rawNode;
    private BitbucketAuthor author;
    private Date date;
    private List<BitbucketNewChangeset> parents;
    public String getSha()
    {
        return sha;
    }
    public void setSha(String sha)
    {
        this.sha = sha;
    }
    public String getMessage()
    {
        return message;
    }
    public void setMessage(String message)
    {
        this.message = message;
    }
    public String getRawNode()
    {
        return rawNode;
    }
    public void setRawNode(String rawNode)
    {
        this.rawNode = rawNode;
    }
    public BitbucketAuthor getAuthor()
    {
        return author;
    }
    public void setAuthor(BitbucketAuthor author)
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
    public List<BitbucketNewChangeset> getParents()
    {
        return parents;
    }
    public void setParents(List<BitbucketNewChangeset> parents)
    {
        this.parents = parents;
    }
}
