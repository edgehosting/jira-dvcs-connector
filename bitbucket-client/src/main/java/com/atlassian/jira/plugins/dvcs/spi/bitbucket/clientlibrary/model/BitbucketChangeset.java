package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 
 * BitbucketChangeset
 * 
 * <pre>
 *  {
 *      "node": "8b2d4d092764",
 *      "files": [
 *          {
 *              "type": "added",
 *              "file": "test1 resource.txt"
 *          }
 *      ],
 *      "raw_author": "jirabitbucketconnector",
 *      "utctimestamp": "2011-12-21 14:17:37+00:00",
 *      "author": "jirabitbucketconnector",
 *      "timestamp": "2011-12-21 15:17:37",
 *      "raw_node": "8b2d4d0927645c4f7bf59a817587e9006458c1b7",
 *      "parents": [],
 *      "branch": "default",
 *      "message": "resource with space in name",
 *      "revision": 0,
 *      "size": -1
 *  }, ...
 * 
 * </pre>
 *
 * 
 * <br /><br />
 * Created on 12.7.2012, 16:30:43
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class BitbucketChangeset implements Serializable
{//TODO timestamp vs utctimestamp
	private static final long serialVersionUID = -1593016501516234658L;

	private String node;
	private List<BitbucketChangesetFile> files;
	private String author;
	private Date timestamp;
	private String branch;
	private String message;
	private Integer revision;
	private Long size;
    private String rawNode;
    private String rawAuthor;
    private Date utctimestamp;
    private List<String> parents;

	public String getNode()
	{
		return node;
	}

	public void setNode(String node)
	{
		this.node = node;
	}

	public List<BitbucketChangesetFile> getFiles()
	{
		return files;
	}

	public void setFiles(List<BitbucketChangesetFile> files)
	{
		this.files = files;
	}

	public String getAuthor()
	{
		return author;
	}

	public void setAuthor(String author)
	{
		this.author = author;
	}

	public Date getTimestamp()
	{
		return timestamp;
	}

	public void setTimestamp(Date timestamp)
	{
		this.timestamp = timestamp;
	}

	public String getBranch()
	{
		return branch;
	}

	public void setBranch(String branch)
	{
		this.branch = branch;
	}

	public String getMessage()
	{
		return message;
	}

	public void setMessage(String message)
	{
		this.message = message;
	}

	public Integer getRevision()
	{
		return revision;
	}

	public void setRevision(Integer revision)
	{
		this.revision = revision;
	}

	public Long getSize()
	{
		return size;
	}

	public void setSize(Long size)
	{
		this.size = size;
	}

    public String getRawNode()
    {
        return rawNode;
    }

    public void setRawNode(String rawNode)
    {
        this.rawNode = rawNode;
    }

    public String getRawAuthor()
    {
        return rawAuthor;
    }

    public void setRawAuthor(String rawAuthor)
    {
        this.rawAuthor = rawAuthor;
    }

    public Date getUtctimestamp()
    {
        return utctimestamp;
    }

    public void setUtctimestamp(Date utctimestamp)
    {
        this.utctimestamp = utctimestamp;
    }

    public List<String> getParents()
    {
        return parents;
    }

    public void setParents(List<String> parents)
    {
        this.parents = parents;
    }
}
